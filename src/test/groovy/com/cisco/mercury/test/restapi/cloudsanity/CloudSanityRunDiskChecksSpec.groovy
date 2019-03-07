package com.cisco.mercury.test.restapi.cloudsanity

import groovy.json.JsonSlurper
import groovyx.net.http.ContentType
import spock.lang.Shared
import spock.lang.Unroll

class CloudSanityRunDiskChecksSpec  extends CloudSanityBaseSpecification {


    @Shared String test_uuid = null
    static Map<String, Map>all_tests

    def setup() {
        control_tests['disk_maintenance_vd_health'] = "PASSED"
        control_tests['disk_maintenance_raid_health'] = "PASSED"
        compute_tests['disk_maintenance_vd_health'] = "PASSED"
        compute_tests['disk_maintenance_raid_health'] = "PASSED"
        management_tests['disk_maintenance_vd_health'] = "PASSED"
        management_tests['disk_maintenance_raid_health'] = "PASSED"
        all_tests = ["management": management_tests,
                     "control": control_tests,
                     "compute": compute_tests,
                     "cephosd": cephosd_tests,
                     "cephmon": cephmon_tests]
    }

    def cleanup() {
        if( test_uuid != null ) {
            waitForTest(test_uuid)
        }
    }

    @Unroll
    def "Test cloud-sanity #role test generates results with disk checks"() {
        given: "A request for a cloud-sanity #role test"
        assert client != null
        def resp = client.post(path: 'cloudsanity/create',
                requestContentType: ContentType.JSON,
                headers: ['Content-Type': "application/json; charset=UTF-8"],
                body: ['cloudsanity_request':
                               ['command': 'create',
                                'action': 'test',
                                'test_name': role,
                                'uuid': '',
                                'run_disk_check': 'True']])
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
        assert test_detail.message.message == "[PASSED] Cloud Sanity " + role.capitalize() + " Checks Passed"

        and: "The results for all tests can be verified"
        def test_result = test_detail.message.results
        def role_results = test_result[role]
        def expected_tests = all_tests[role]

        assert role_results.size() == expected_tests.size()
        role_results.each { String test, String result ->
            assert expected_tests.keySet().contains(test)
            assert valid_test_results.contains((String)result)
            assert expected_tests[test] == result
        }

        then: "The results can be deleted"
        def del_resp = client.delete(path: 'cloudsanity/delete/',
                requestContentType: ContentType.TEXT,
                headers: ['Content-Type': "application/json"],
                query: ['uuid': test_uuid ])

        and: "The delete verified"
        assert del_resp.status == OK

        where:
        role << ['management','compute', 'control']
    }

    def "Test cloud-sanity all test generates results  with disk checks"() {
        given: "A request for a cloud-sanity all test"
        assert client != null
        def resp = client.post(path: 'cloudsanity/create',
                requestContentType: ContentType.JSON,
                headers: ['Content-Type': "application/json; charset=UTF-8"],
                body: ['cloudsanity_request':
                               ['command': 'create',
                                'action': 'test',
                                'test_name': 'all',
                                'uuid': '',
                                'run_disk_check': 'True']])
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
}

