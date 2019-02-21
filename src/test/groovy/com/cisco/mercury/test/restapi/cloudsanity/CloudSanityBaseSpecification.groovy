package com.cisco.mercury.test.restapi.cloudsanity

import com.cisco.mercury.test.restapi.CvimRestApiSpecification
import groovy.json.JsonSlurper
import groovyx.net.http.ContentType

class CloudSanityBaseSpecification extends CvimRestApiSpecification {

    static Map cloudsanity_status = ["RUNNING": "cloudsanity_running",
                                     "COMPLETE": "cloudsanity_completed",
                                     "FAILED": "cloudsanity_failed"]

    static List<String> control_test_names = ["ping_all_controller_nodes",
                                              "check_rabbitmq_is_running",
                                              "check_rabbitmq_cluster_status",
                                              "check_nova_service_list",
                                              "container_version_check",
                                              "ping_internal_vip",
                                              "disk_maintenance_raid_health",
                                              "check_mariadb_cluster_size",
                                              "disk_maintenance_vd_health"]

    static List<String> compute_test_names = ["check_nova_hypervisor_list",
                                              "disk_maintenance_raid_health",
                                              "container_version_check",
                                              "ping_all_compute_nodes",
                                              "disk_maintenance_vd_health"]

    static List<String> cephmon_test_names = ["check_cephmon_status",
                                              "ceph_cluster_check",
                                              "check_cephmon_results",
                                              "check_cephmon_is_running"]

    static List<String> cephosd_test_names = ["ping_all_storage_nodes",
                                              "check_osd_result_without_osdinfo",
                                              "osd_overall_status",
                                              "check_osd_result_with_osdinfo"]

    static List<String> management_test_names = ["disk_maintenance_vd_health",
                                                 "disk_maintenance_raid_health",
                                                 "container_version_check"]


    static List<String> valid_test_results = ["PASSED", "FAILED", "SKIPPED"]

    def setupSpec() {
        deleteAllTestResults()
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
