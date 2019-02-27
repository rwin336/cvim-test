package com.cisco.mercury.test.restapi.osdmgmt

import groovy.json.JsonSlurper
import groovyx.net.http.ContentType
import spock.lang.Shared

class OsdMgmtCheckOsdsSpec extends OsdMgmtBaseSpec {

    @Shared String test_uuid = null

    def setup() {}

    def cleanup() {
        if( test_uuid != null ) {
            waitForTest(test_uuid)
        }
    }

    def "Test OSDMgmt create check-osds test"() {
        given: "A valid Mercury REST API Client"
        assert client != null

        when: "The osdmgmt create command is POSTed for a check-osds test"
        def resp = client.post(path: 'osdmgmt/check_osds/',
                requestContentType: ContentType.JSON,
                headers: ['Content-Type': "application/json; charset=UTF-8"],
                body: ['osdmgmt_request':
                               ['command': 'create',
                                'action': 'check-osds',
                                'locator': 'false',
                                'json_display': 'false',
                                'servers': '',
                                'osd': '',
                                'uuid': '']])

        then: "A check-osds test is created"
        assert resp.status == CREATED

        when: "The test completes"
        def osdmgmt_request = getOsdMgmtRequest(resp)
        test_uuid = osdmgmt_request['uuid']
        waitForTest(test_uuid)

        then: "The test UUID is not null"
        assert test_uuid != null

        then: "The results can be obtained"
        def show_resp = client.get(path: 'osdmgmt/show/',
                requestContentType: ContentType.TEXT,
                headers: ['Content-Type': "application/json"],
                query: ['uuid': test_uuid ])

        assert show_resp.status == OK

        and: "The UUID can be verified"
        def show_uuid = show_resp.responseData['uuid']
        assert show_uuid == test_uuid

        and: "The test details can be verified"
        def jsonSlurper = new JsonSlurper()
        def test_detail = jsonSlurper.parseText((String)show_resp.responseData['osdmgmt_result'])

        assert test_detail.status == "PROCESSED"
        assert test_detail.message.size() == 2

        then: "The results can be deleted"
        def del_resp = client.delete(path: 'osdmgmt/delete/',
                requestContentType: ContentType.TEXT,
                headers: ['Content-Type': "application/json"],
                query: ['uuid': test_uuid ])

        and: "The delete verified"
        assert del_resp.status == OK

    }
}
