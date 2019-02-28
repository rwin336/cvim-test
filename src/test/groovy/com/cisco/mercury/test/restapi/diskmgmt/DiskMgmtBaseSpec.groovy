package com.cisco.mercury.test.restapi.diskmgmt

import com.cisco.mercury.test.restapi.CvimRestApiSpecification
import groovy.json.JsonSlurper
import groovyx.net.http.ContentType
import org.apache.commons.collections.functors.FalsePredicate


class DiskMgmtBaseSpec extends CvimRestApiSpecification {

    static Map diskmgmt_status = ["RUNNING": "diskmgmt_running",
                                 "COMPLETE": "diskmgmt_completed",
                                 "FAILED": "diskmgmt_failed"]

    def setupSpec() {
        deleteAllTestResults()
    }

    def getDiskMgmtRequest( response ) {
        def jsonSlurper = new JsonSlurper()
        String base_str = response.responseData['diskmgmt_request']
        String clean_json = base_str.replaceAll("u'", "'")
        String json_str = clean_json.replaceAll("'",'"')
        def diskmgmt_request = jsonSlurper.parseText(json_str)
        return diskmgmt_request
    }

    def parseDiskMgmtJsonStr( String base_str ) {
        def json_slurper = new JsonSlurper()
        String clean_json = base_str.replaceAll("u'", "'")
        String json_str = clean_json.replaceAll("'", '"')
        return json_slurper.parseText(json_str)
    }

    def createCheckDisks(String role, String servers="") {
        def resp = client.post(path: 'diskmgmt/check_disks/',
                requestContentType: ContentType.JSON,
                headers: ['Content-Type': "application/json; charset=UTF-8"],
                body: ['diskmgmt_request':
                               ['command': 'create',
                                'action': 'check-disks',
                                'role': role,
                                'locator': 'false',
                                'json_display': 'false',
                                'servers': servers,
                                'uuid': '']])
        return resp
    }

    def getShowResult(String test_uuid) {
        def show_resp = client.get(path: 'diskmgmt/show/',
                requestContentType: ContentType.TEXT,
                headers: ['Content-Type': "application/json"],
                query: ['uuid': test_uuid ])
        return show_resp
    }

    def deleteDiskMgmtJob(String test_uuid) {
        def del_resp = client.delete(path: 'diskmgmt/delete/',
                requestContentType: ContentType.TEXT,
                headers: ['Content-Type': "application/json"],
                query: ['uuid': test_uuid ])
        return del_resp
    }

    def getTestStatus(String uuid) {
        String status = "UNKNOWN"
        def show_resp = client.get(path: 'diskmgmt/show/',
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
        if ( uuidHasResults(uuid)) {
            while (((current_time - start_time) / 1000) < timeout) {
                String current_status = getTestStatus(uuid)

                if (current_status == diskmgmt_status[status] ||
                        current_status == diskmgmt_status["FAILED"] || current_status == "NOT_FOUND") {
                    test_state = current_status
                    break
                }
                sleep(poll * 1000)
                current_time = System.currentTimeMillis()
            }
        }
        return test_state
    }

    def uuidHasResults(String uuid) {
        boolean status = false

        def resp = client.get(path: 'diskmgmt',
                requestContentType: ContentType.JSON,
                headers: ['Content-Type': "application/json"])

        if (resp.status == OK) {
            resp.responseData.each() { it ->
                if ((String) it.key == uuid) {
                    status = true
                }
            }
        }
        return status
    }

    def deleteAllTestResults() {
        def resp = client.get(path: 'diskmgmt',
                requestContentType: ContentType.JSON,
                headers: ['Content-Type': "application/json"] )

        assert resp.status == OK

        resp.responseData.each() { it ->
            waitForTest((String)it.key)
            def del_resp = client.delete(path: 'diskmgmt/delete/',
                    requestContentType: ContentType.TEXT,
                    headers: ['Content-Type': "application/json"],
                    query: ['uuid': it.key ])
        }
    }
}
