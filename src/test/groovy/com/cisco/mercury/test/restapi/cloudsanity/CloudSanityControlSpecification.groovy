package com.cisco.mercury.test.restapi.cloudsanity

import groovyx.net.http.ContentType
import groovy.json.*
import spock.lang.Shared

class CloudSanityControlSpecification extends CloudSanityBaseSpecification  {

    @Shared String test_uuid = null

    def setup() {}

    def cleanup() {
        if( test_uuid != null ) {
            waitForTest(test_uuid)
        }
    }

    def "Test cloud-sanity create control test"() {
        given: "A valid Mercury REST API Client"
        assert client != null

        when: "The cloud-sanity create command is POSTed for a control test"
        def resp = client.post(path: 'cloudsanity/create',
                requestContentType: ContentType.JSON,
                headers: ['Content-Type': "application/json; charset=UTF-8"],
                body: ['cloudsanity_request':
                               ['command': 'create', 'action': 'test', 'test_name': 'control', 'uuid': '']])

        then: "A control test is created"
        assert resp.status == CREATED

        when: "The test completes"
        def cloudsanity_request = getCloudSanityRequest(resp)
        test_uuid = cloudsanity_request['uuid']
        waitForTest(test_uuid)

        then: "The test UUID is not null"
        assert test_uuid != null
    }

    def "Test cloud-sanity control test generates results"() {
        given: "A request for a cloud-sanity control test"
        assert client != null
        def resp = client.post(path: 'cloudsanity/create',
                requestContentType: ContentType.JSON,
                headers: ['Content-Type': "application/json; charset=UTF-8"],
                body: ['cloudsanity_request':
                               ['command': 'create', 'action': 'test', 'test_name': 'control', 'uuid': '']])
        assert resp.status == CREATED

        when: "The test completes"
        def cloudsanity_request = getCloudSanityRequest(resp)
        test_uuid = cloudsanity_request['uuid']
        waitForTest(test_uuid)

        then: "The results can be obtained"
        def show_resp = client.get(path: 'cloudsanity/show/',
                requestContentType: ContentType.TEXT,
                headers: ['Content-Type': "application/json"],
                query: ['uuid': test_uuid ])

        assert show_resp.status == OK

        and: "The UUID can be verified"
        def show_uuid = show_resp.responseData['uuid']
        assert show_uuid == test_uuid

        and: "The test details can be verified"
        def jsonSlurper = new JsonSlurper()
        def test_detail = jsonSlurper.parseText((String)show_resp.responseData['cloudsanity_result'])

        assert test_detail.status == "PROCESSED"
        assert test_detail.message.size() == 3
        assert test_detail.message.status == 'Pass'
        assert test_detail.message.message == "[PASSED] Cloud Sanity Control Checks Passed"

        and: "The results for control tests can be verified"
        def test_result = test_detail.message.results
        def control_results = test_result.control

        assert control_results.size() == control_tests.size()

        control_results.each { String test, String result ->
            assert control_tests.keySet().contains((String)test)
            assert valid_test_results.contains((String)result)
            assert control_tests[test] == result
        }

        then: "The results can be deleted"
        def del_resp = client.delete(path: 'cloudsanity/delete/',
                requestContentType: ContentType.TEXT,
                headers: ['Content-Type': "application/json"],
                query: ['uuid': test_uuid ])

        and: "The delete verified"
        assert del_resp.status == OK
    }

    def "Test create control test is blocked when another cloud-sanity test running"() {
        given: "A cloud-sanity test already running"
        assert client != null
        def resp = client.post(path: 'cloudsanity/create',
                requestContentType: ContentType.JSON,
                headers: ['Content-Type': "application/json; charset=UTF-8"],
                body: ['cloudsanity_request':
                               ['command': 'create', 'action': 'test', 'test_name': 'control', 'uuid': '']])
        assert resp.status == CREATED
        def cloudsanity_request = getCloudSanityRequest(resp)
        test_uuid = cloudsanity_request['uuid']
        waitForTest(test_uuid, "RUNNING")

        when: "A new request for cloud-sanity control test is create"
        def new_resp = client.post(path: 'cloudsanity/create',
                requestContentType: ContentType.JSON,
                headers: ['Content-Type': "application/json; charset=UTF-8"],
                body: ['cloudsanity_request':
                               ['command': 'create', 'action': 'test', 'test_name': 'control', 'uuid': '']])


        then: "The new request is rejected"
        assert new_resp.status == CONFLICT
        assert new_resp.responseData["faultstring"] == "CloudSanity Operation is already in progress. Please try after sometime."
        waitForTest(test_uuid)

        then: "The results can be deleted"
        def del_resp = client.delete(path: 'cloudsanity/delete/',
                requestContentType: ContentType.TEXT,
                headers: ['Content-Type': "application/json"],
                query: ['uuid': test_uuid ])

        and: "The delete verified"
        assert del_resp.status == OK
    }

    def "Test delete is blocked util previous test is complete"() {
        given: "A cloud-sanity test already running"
        assert client != null
        def resp = client.post(path: 'cloudsanity/create',
                requestContentType: ContentType.JSON,
                headers: ['Content-Type': "application/json; charset=UTF-8"],
                body: ['cloudsanity_request':
                               ['command': 'create', 'action': 'test', 'test_name': 'control', 'uuid': '']])
        assert resp.status == CREATED
        def cloudsanity_request = getCloudSanityRequest(resp)
        test_uuid = cloudsanity_request['uuid']
        waitForTest(test_uuid, "RUNNING")

        when: "An attempt to delete the test is made"
        def del_resp = client.delete(path: 'cloudsanity/delete/',
                requestContentType: ContentType.TEXT,
                headers: ['Content-Type': "application/json"],
                query: ['uuid': test_uuid ])

        then: "The delete request is rejected"
        assert del_resp.status == CONFLICT

        and: "The first test completes successfully"
        waitForTest(test_uuid)

        then: "The results can be obtained"
        def show_resp = client.get(path: 'cloudsanity/show/',
                requestContentType: ContentType.TEXT,
                headers: ['Content-Type': "application/json"],
                query: ['uuid': test_uuid ])

        assert show_resp.status == OK
        def show_uuid = show_resp.responseData['uuid']
        assert show_uuid == test_uuid

        and: "The test details can be verified"
        def jsonSlurper = new JsonSlurper()
        def test_detail = jsonSlurper.parseText((String)show_resp.responseData['cloudsanity_result'])

        assert test_detail.status == "PROCESSED"
        assert test_detail.message.size() == 3
        assert test_detail.message.status == 'Pass'
        assert test_detail.message.message == "[PASSED] Cloud Sanity Control Checks Passed"

        and: "The results for control tests can be verified"
        def test_result = test_detail.message.results
        def control_results = test_result.control

        assert control_results.size() == control_tests.size()

        control_results.each { String test, String result ->
            assert control_tests.keySet().contains(test)
            assert valid_test_results.contains(result)
            assert control_tests[test] == result
        }

        then: "The results can be deleted"
        def delete_resp = client.delete(path: 'cloudsanity/delete/',
                requestContentType: ContentType.TEXT,
                headers: ['Content-Type': "application/json"],
                query: ['uuid': test_uuid ])

        and: "The delete verified"
        assert delete_resp.status == OK
    }
}
