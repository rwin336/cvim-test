package com.cisco.mercury.test.restapi.cloudsanity

import groovy.json.JsonSlurper
import groovyx.net.http.ContentType
import spock.lang.Shared

class CloudSanityAllSpecification extends CloudSanityBaseSpecification {

    @Shared String test_uuid = null

    def setup() {}

    def cleanup() {
        if( test_uuid != null ) {
            waitForTest(test_uuid)
        }
    }

    def "Test cloud-sanity create all test"() {
        given: "A valid Mercury REST API Client"
        assert client != null

        when: "The cloud-sanity create command is POSTed for a all test"
        def resp = client.post(path: 'cloudsanity/create',
                requestContentType: ContentType.JSON,
                headers: ['Content-Type': "application/json; charset=UTF-8"],
                body: ['cloudsanity_request':
                               ['command': 'create', 'action': 'test', 'test_name': 'all', 'uuid': '']])

        then: "A all test is created"
        assert resp.status == CREATED

        when: "The test completes"
        def cloudsanity_request = getCloudSanityRequest(resp)
        test_uuid = cloudsanity_request['uuid']
        waitForTest(test_uuid)

        then: "The test UUID is not null"
        assert test_uuid != null
    }

    def "Test cloud-sanity all test generates results"() {
        given: "A request for a cloud-sanity all test"
        assert client != null
        def resp = client.post(path: 'cloudsanity/create',
                requestContentType: ContentType.JSON,
                headers: ['Content-Type': "application/json; charset=UTF-8"],
                body: ['cloudsanity_request':
                               ['command': 'create', 'action': 'test', 'test_name': 'all', 'uuid': '']])
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
        assert test_detail.message.message == "[PASSED] Cloud Sanity All Checks Passed"

        and: "The results for all tests can be verified"
        def test_result = test_detail.message.results
        def control_results = test_result.control
        def management_results = test_result.management
        def compute_results = test_result.compute
        def cephmon_results = test_result.cephmon
        def cephosd_results = test_result.cephosd

        assert control_results.size() == control_tests.size()
        control_results.each { String test, String result ->
            assert control_tests.keySet().contains(test)
            assert valid_test_results.contains((String)result)
            assert control_tests[test] == result
        }

        assert compute_results.size() == compute_tests.size()
        compute_results.each { String test, String result ->
            assert compute_tests.keySet().contains(test)
            assert valid_test_results.contains(result)
            assert compute_tests[test] == result
        }

        assert management_results.size() == management_tests.size()
        management_results.each { String test, String result ->
            assert management_tests.keySet().contains(test)
            assert valid_test_results.contains(result)
            assert management_tests[test] == result
        }

        assert cephosd_results.size() == cephosd_tests.size()
        cephosd_results.each { String test, String result ->
            assert cephosd_tests.keySet().contains(test)
            assert valid_test_results.contains(result)
            assert cephosd_tests[test] == result
        }

        assert cephmon_results.size() == cephmon_tests.size()
        cephmon_results.each { String test, String result ->
            assert cephmon_tests.keySet().contains(test)
            assert valid_test_results.contains(result)
            assert cephmon_tests[test] == result
        }

        then: "The results can be deleted"
        def del_resp = client.delete(path: 'cloudsanity/delete/',
                requestContentType: ContentType.TEXT,
                headers: ['Content-Type': "application/json"],
                query: ['uuid': test_uuid ])

        and: "The delete verified"
        assert del_resp.status == OK
    }

    def "Test create all test is blocked when another cloud-sanity test running"() {
        given: "A cloud-sanity test already running"
        assert client != null
        def resp = client.post(path: 'cloudsanity/create',
                requestContentType: ContentType.JSON,
                headers: ['Content-Type': "application/json; charset=UTF-8"],
                body: ['cloudsanity_request':
                               ['command': 'create', 'action': 'test', 'test_name': 'all', 'uuid': '']])
        assert resp.status == CREATED
        def cloudsanity_request = getCloudSanityRequest(resp)
        test_uuid = cloudsanity_request['uuid']
        waitForTest(test_uuid, "RUNNING")

        when: "A new request for cloud-sanity all test is create"
        def new_resp = client.post(path: 'cloudsanity/create',
                requestContentType: ContentType.JSON,
                headers: ['Content-Type': "application/json; charset=UTF-8"],
                body: ['cloudsanity_request':
                               ['command': 'create', 'action': 'test', 'test_name': 'all', 'uuid': '']])


        then: "The new request is rejected"
        assert new_resp.status == CONFLICT
        assert new_resp.responseData["faultstring"] == "CloudSanity Operation is already in progress. Please try after sometime."
        waitForTest(test_uuid)
    }

    def "Test delete is blocked util previous test is complete"() {
        given: "A cloud-sanity test already running"
        assert client != null
        def resp = client.post(path: 'cloudsanity/create',
                requestContentType: ContentType.JSON,
                headers: ['Content-Type': "application/json; charset=UTF-8"],
                body: ['cloudsanity_request':
                               ['command': 'create', 'action': 'test', 'test_name': 'all', 'uuid': '']])
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
        assert test_detail.message.message == "[PASSED] Cloud Sanity All Checks Passed"

        and: "The results for all tests can be verified"
        def test_result = test_detail.message.results
        def control_results = test_result.control
        def management_results = test_result.management
        def compute_results = test_result.compute
        def cephmon_results = test_result.cephmon
        def cephosd_results = test_result.cephosd

        assert control_results.size() == control_tests.size()
        control_results.each { String test, String result ->
            assert control_tests.keySet().contains(test)
            assert valid_test_results.contains(result)
            assert control_tests[test] == result
        }

        assert compute_results.size() == compute_tests.size()
        compute_results.each { String test, String result ->
            assert compute_tests.keySet().contains((String)test)
            assert valid_test_results.contains((String)result)
            assert compute_tests[test] == result
        }

        assert management_results.size() == management_tests.size()
        management_results.each { String test, String result ->
            assert management_tests.keySet().contains(test)
            assert valid_test_results.contains(result)
            assert management_tests[test] == result
        }

        assert cephosd_results.size() == cephosd_tests.size()
        cephosd_results.each { String test, String result ->
            assert cephosd_tests.keySet().contains(test)
            assert valid_test_results.contains(result)
            assert cephosd_tests[test] == result
        }

        assert cephmon_results.size() == cephmon_tests.size()
        cephmon_results.each { String test, String result ->
            assert cephmon_tests.keySet().contains(test)
            assert valid_test_results.contains(result)
            assert cephmon_tests[test] == result
        }

    }

}
