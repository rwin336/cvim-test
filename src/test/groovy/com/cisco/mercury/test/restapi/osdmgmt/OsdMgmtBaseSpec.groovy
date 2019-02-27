package com.cisco.mercury.test.restapi.osdmgmt

import com.cisco.mercury.test.restapi.CvimRestApiSpecification
import groovy.json.JsonSlurper
import groovyx.net.http.ContentType
import org.apache.commons.collections.functors.FalsePredicate

class OsdMgmtBaseSpec extends CvimRestApiSpecification {

    static Map osdmgmt_status = ["RUNNING": "osdmgmt_running",
                                 "COMPLETE": "osdmgmt_completed",
                                 "FAILED": "osdmgmt_failed"]

    def setupSpec() {
        deleteAllTestResults()
    }

    def getOsdMgmtRequest( response ) {
        def jsonSlurper = new JsonSlurper()
        String base_str = response.responseData['osdmgmt_request']
        String clean_json = base_str.replaceAll("u'", "'")
        String json_str = clean_json.replaceAll("'",'"')
        def osdmgmt_request = jsonSlurper.parseText(json_str)
        return osdmgmt_request
    }

    def getTestStatus(String uuid) {
        String status = "UNKNOWN"
        def show_resp = client.get(path: 'osdmgmt/show/',
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

                if (current_status == osdmgmt_status[status] ||
                        current_status == osdmgmt_status["FAILED"] || current_status == "NOT_FOUND") {
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

        def resp = client.get(path: 'osdmgmt',
                requestContentType: ContentType.JSON,
                headers: ['Content-Type': "application/json"] )

        assert resp.status == OK

        resp.responseData.each() { it ->
            if ((String)it.key == uuid) {
                status = true
            }
        }
        return status
    }

    def deleteAllTestResults() {
        def resp = client.get(path: 'osdmgmt',
                requestContentType: ContentType.JSON,
                headers: ['Content-Type': "application/json"] )

        assert resp.status == OK

        resp.responseData.each() { it ->
            waitForTest((String)it.key)
            def del_resp = client.delete(path: 'osdmgmt/delete/',
                    requestContentType: ContentType.TEXT,
                    headers: ['Content-Type': "application/json"],
                    query: ['uuid': it.key ])
        }
    }

}
