package com.cisco.mercury.test.restapi.cloudsanity

import com.cisco.mercury.test.restapi.CvimRestApiSpecification
import groovy.json.JsonSlurper
import groovyx.net.http.ContentType

class CloudSanityBaseSpecification extends CvimRestApiSpecification {

    static Map cloudsanity_status = ["RUNNING": "cloudsanity_running",
                                     "COMPLETE": "cloudsanity_completed",
                                     "FAILED": "cloudsanity_failed"]

    static Map<String, String> control_tests = ["ping_all_controller_nodes": "PASSED",
                                                "check_rabbitmq_is_running": "PASSED",
                                                "check_rabbitmq_cluster_status": "PASSED",
                                                "check_rabbitmq_cluster_size": "PASSED",
                                                "check_nova_service_list": "PASSED",
                                                "container_version_check": "PASSED",
                                                "ping_internal_vip": "PASSED",
                                                "ping_all_cephosd_nodes": "SKIPPED",
                                                "disk_maintenance_raid_health": "SKIPPED",
                                                "check_mariadb_cluster_size": "PASSED",
                                                "disk_maintenance_vd_health": "SKIPPED",
                                                "percent_used_on_var_check": "PASSED"]

    static Map<String, String>  compute_tests = ["check_nova_hypervisor_list": "PASSED",
                                                 "disk_maintenance_raid_health": "SKIPPED",
                                                 "container_version_check": "PASSED",
                                                 "ping_all_compute_nodes": "PASSED",
                                                 "disk_maintenance_vd_health": "SKIPPED",
                                                 "percent_used_on_var_check": "PASSED"]

    static Map<String, String>  cephmon_tests = ["check_cephmon_status": "PASSED",
                                                 "ceph_cluster_check": "PASSED",
                                                 "check_cephmon_results": "PASSED",
                                                 "check_cephmon_is_running": "PASSED"]

    static Map<String, String>  cephosd_tests = ["ping_all_storage_nodes": "PASSED",
                                                 "check_osd_result_without_osdinfo": "PASSED",
                                                 "osd_overall_status": "PASSED",
                                                 "check_osd_result_with_osdinfo": "PASSED"]

    static Map<String, String>  management_tests = ["disk_maintenance_vd_health": "SKIPPED",
                                                    "disk_maintenance_raid_health": "SKIPPED",
                                                    "container_version_check": "PASSED",
                                                    "percent_used_on_var_check": "PASSED"]

    static List<String> valid_test_results = ["PASSED", "FAILED", "SKIPPED", "WARNING"]

    def setupSpec() {
        deleteAllTestResults()
        if( pod_type == "ceph") {
            control_tests['ping_all_cephosd_nodes'] = 'PASSED'
            control_tests['ping_all_controller_nodes'] = 'SKIPPED'
        }
        if( pod_series == "B-Series") {
            cephosd_tests['osd_overall_status'] = 'SKIPPED'
        }
    }

    def getCloudSanityRequest( response ) {
        def jsonSlurper = new JsonSlurper()
        String base_str = response.responseData['cloudsanity_request']
        String clean_json = base_str.replaceAll("u'", "'")
        String json_str = clean_json.replaceAll("'",'"')
        def cloudsanity_request = jsonSlurper.parseText(json_str)
        return cloudsanity_request
    }

    def getTestStatus(String uuid) {
        String status = "UNKNOWN"
        def show_resp = client.get(path: 'cloudsanity/show/',
                requestContentType: ContentType.TEXT,
                headers: ['Content-Type': "application/json"],
                query: ['uuid': uuid ])

        if(show_resp.status == OK) {
            status = show_resp.responseData['status']
        }

        if(show_resp.status == NOT_FOUND ) {
            status = "NOT_FOUND"
        }
        return status
    }

    def waitForTest( String uuid, String status='COMPLETE', int poll=5, int timeout=600 ) {
        long start_time = System.currentTimeMillis()
        sleep(1 * 1000)
        long current_time = System.currentTimeMillis()

        String test_state = 'UNKNOWN'
        while(((current_time - start_time) / 1000) < timeout) {
            String current_status = getTestStatus(uuid)

            if( current_status == cloudsanity_status[status] ||
                    current_status == cloudsanity_status["FAILED"] || current_status == "NOT_FOUND") {
                test_state = current_status
                break
            }
            sleep(poll * 1000)
            current_time = System.currentTimeMillis()
        }
        return test_state
    }

    def deleteAllTestResults() {
        def resp = client.get(path: 'cloudsanity',
                requestContentType: ContentType.JSON,
                headers: ['Content-Type': "application/json"] )

        assert resp.status == OK

        resp.responseData.each() { it ->
            waitForTest((String)it.key)
            def del_resp = client.delete(path: 'cloudsanity/delete/',
                    requestContentType: ContentType.TEXT,
                    headers: ['Content-Type': "application/json"],
                    query: ['uuid': it.key ])
        }
    }
}
