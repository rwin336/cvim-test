package com.cisco.mercury.test.restapi.cloudsanity

import com.cisco.mercury.test.restapi.MercRestApiSpecification
import groovyx.net.http.*
import spock.lang.*

class CloudSanityListSpecification extends CloudSanityBaseSpecification {

    @Shared String test_uuid = null
    @Shared int num_cloudsanity_jobs = 0
    @Shared Map<String, Integer> list_tests = ['cephmon': 6,
                                               'cephosd': 5,
                                               'control': 4,
                                               'compute': 3,
                                               'management': 2,
                                               'all': 1]

    def setupSpec() {
        list_tests.each { test_name, test_runs ->
            for (int i = 0; i < (Integer) test_runs; i++) {
                def resp = client.post(path: 'cloudsanity/create',
                        requestContentType: ContentType.JSON,
                        headers: ['Content-Type': "application/json; charset=UTF-8"],
                        body: ['cloudsanity_request':
                                       ['command': 'create', 'action': 'test', 'test_name': test_name, 'uuid': '']])

                assert resp.status == CREATED
                def cloudsanity_request = getCloudSanityRequest(resp)
                test_uuid = cloudsanity_request['uuid']
                waitForTest(test_uuid)
                num_cloudsanity_jobs++
            }
        }
    }

    def setup() {}

    def cleanup() {
        if( test_uuid != null ) {
            waitForTest(test_uuid)
        }
    }

    def "Verify main cloud-sanity endpoint returns list of all cloud-sanity jobs"() {
        given: "A PoD that has already run several cloud-sanity jobs"
        assert num_cloudsanity_jobs > 0

        when: "A request to list all via the API is performed"
        def resp = client.get(path: 'cloudsanity',
                requestContentType: ContentType.JSON,
                headers: ['Content-Type': "application/json"])

        then: "The response is OK and the list contains the jobs run"
        assert resp.status == OK
        assert resp.responseData.size() == num_cloudsanity_jobs
        Map actual_list_tests = ['cephmon': 0,
                                 'cephosd': 0,
                                 'control': 0,
                                 'compute': 0,
                                 'management': 0,
                                 'all': 0]

        resp.responseData.each { uuid, test_data ->
            assert test_data.uuid == uuid
            assert test_data.status == cloudsanity_status['COMPLETE']
            int current_num = (int)actual_list_tests[(String)test_data.test_name]
            actual_list_tests[(String)test_data.test_name] = current_num + 1
        }

        list_tests.each { test_name, expected_test_runs ->
            assert expected_test_runs == actual_list_tests[test_name]
        }
    }

    def "Verify cloud-sanity list all returns all cloud-sanity jobs"() {
        given: "A PoD that has already run several cloud-sanity jobs"
        assert num_cloudsanity_jobs > 0

        when: "A request to list all via the API is performed"
        def resp = client.get(path: 'cloudsanity/list/',
                requestContentType: ContentType.JSON,
                query: [test_name: 'all'],
                headers: ['Content-Type': "application/json"] )

        then: "The response is OK and the list contains the jobs run"
        assert resp.status == OK
        assert resp.responseData.size() == num_cloudsanity_jobs
        Map actual_list_tests = ['cephmon': 0,
                                 'cephosd': 0,
                                 'control': 0,
                                 'compute': 0,
                                 'management': 0,
                                 'all': 0]

        resp.responseData.each { uuid, test_data ->
            assert test_data.uuid == uuid
            assert test_data.status == cloudsanity_status['COMPLETE']
            int current_num = (int)actual_list_tests[(String)test_data.test_name]
            actual_list_tests[(String)test_data.test_name] = current_num + 1
        }

        list_tests.each { test_name, expected_test_runs ->
            assert expected_test_runs == actual_list_tests[test_name]
        }
    }

    def "Verify cloudsanity/list/?test_name=control endpoint returns a list of control jobs "() {
        given: "A PoD that has already run several cloud-sanity jobs"
        assert num_cloudsanity_jobs > list_tests['control']

        when: "Performing GET on cloudsanity/list/?test_name=control returns control jobs"
        def resp = client.get(path: 'cloudsanity/list/',
                requestContentType: ContentType.JSON,
                query: [test_name: 'control'],
                headers: ['Content-Type': "application/json"] )

        then: "The response is OK and a list of control only jobs returned"
        assert resp.status == OK
        assert resp.responseData.size() == list_tests['control']

        Map actual_list_tests = ['cephmon': 0,
                                 'cephosd': 0,
                                 'control': 0,
                                 'compute': 0,
                                 'management': 0,
                                 'all': 0]

        resp.responseData.each { uuid, test_data ->
            assert test_data.uuid == uuid
            assert test_data.status == cloudsanity_status['COMPLETE']
            int current_num = (int)actual_list_tests[(String)test_data.test_name]
            actual_list_tests[(String)test_data.test_name] = current_num + 1
        }

        assert list_tests['control'] == actual_list_tests['control']
    }

    def "Verify cloudsanity/list/?test_name=compute endpoint returns a list of compute jobs"() {
        given: "A PoD that has already run several cloud-sanity jobs"
        assert num_cloudsanity_jobs >= list_tests['compute']

        when: "Performing GET on cloudsanity/list/?test_name=compute endpoint returns compute jobs"
        def resp = client.get(path: 'cloudsanity/list/',
                requestContentType: ContentType.JSON,
                query: [test_name: 'compute'],
                headers: ['Content-Type': "application/json"] )

        then: "The response is OK and an empty list is returned"
        assert resp.status == OK
        assert resp.responseData.size() == list_tests['compute']

        Map actual_list_tests = ['cephmon': 0,
                                 'cephosd': 0,
                                 'control': 0,
                                 'compute': 0,
                                 'management': 0,
                                 'all': 0]

        resp.responseData.each { uuid, test_data ->
            assert test_data.uuid == uuid
            assert test_data.status == cloudsanity_status['COMPLETE']
            int current_num = (int)actual_list_tests[(String)test_data.test_name]
            actual_list_tests[(String)test_data.test_name] = current_num + 1
        }

        assert list_tests['compute'] == actual_list_tests['compute']
    }

    def "Verify cloudsanity/list/?test_name=cephmon endpoint returns a list of cephmon jobs"() {
        given: "A PoD that has already run several cloud-sanity jobs"
        assert num_cloudsanity_jobs >= list_tests['cephmon']

        when: "Performing GET on cloudsanity/list/?test_name=cephmon endpoint returns cephmon jobs"
        def resp = client.get(path: 'cloudsanity/list/',
                requestContentType: ContentType.JSON,
                query: [test_name: 'cephmon'],
                headers: ['Content-Type': "application/json"] )

        then: "The response is OK and an empty list is returned"
        assert resp.status == OK
        assert resp.responseData.size() == list_tests['cephmon']

        Map actual_list_tests = ['cephmon': 0,
                                 'cephosd': 0,
                                 'control': 0,
                                 'compute': 0,
                                 'management': 0,
                                 'all': 0]

        resp.responseData.each { uuid, test_data ->
            assert test_data.uuid == uuid
            assert test_data.status == cloudsanity_status['COMPLETE']
            int current_num = (int)actual_list_tests[(String)test_data.test_name]
            actual_list_tests[(String)test_data.test_name] = current_num + 1
        }

        assert list_tests['cephmon'] == actual_list_tests['cephmon']
    }

    def "Verify cloudsanity/list/?test_name=cephosd endpoint returns a list of cephosd jobs"() {
        given: "A PoD that has already run several cloud-sanity jobs"
        assert num_cloudsanity_jobs >= list_tests['cephosd']

        when: "Performing GET on cloudsanity/list/?test_name=cephosd endpoint returns cephosd jobs"
        def resp = client.get(path: 'cloudsanity/list/',
                requestContentType: ContentType.JSON,
                query: [test_name: 'cephosd'],
                headers: ['Content-Type': "application/json"] )

        then: "The response is OK and an empty list is returned"
        assert resp.status == OK
        assert resp.responseData.size() == list_tests['cephosd']

        Map actual_list_tests = ['cephmon': 0,
                                 'cephosd': 0,
                                 'control': 0,
                                 'compute': 0,
                                 'management': 0,
                                 'all': 0]

        resp.responseData.each { uuid, test_data ->
            assert test_data.uuid == uuid
            assert test_data.status == cloudsanity_status['COMPLETE']
            int current_num = (int)actual_list_tests[(String)test_data.test_name]
            actual_list_tests[(String)test_data.test_name] = current_num + 1
        }

        assert list_tests['cephosd'] == actual_list_tests['cephosd']

    }

    def "Verify cloudsanity/list/?test_name=management endpoint returns a list management jobs"() {
        given: "A PoD that has already run several cloud-sanity jobs"
        assert num_cloudsanity_jobs >= list_tests['management']

        when: "Performing GET on cloudsanity/list/?test_name=management endpoint "
        def resp = client.get(path: 'cloudsanity/list/',
                requestContentType: ContentType.JSON,
                query: [test_name: 'management'],
                headers: ['Content-Type': "application/json"] )

        then: "The response is OK and an empty list is returned"
        assert resp.status == OK
        assert resp.responseData.size() == list_tests['management']

        Map actual_list_tests = ['cephmon': 0,
                                 'cephosd': 0,
                                 'control': 0,
                                 'compute': 0,
                                 'management': 0,
                                 'all': 0]

        resp.responseData.each { uuid, test_data ->
            assert test_data.uuid == uuid
            assert test_data.status == cloudsanity_status['COMPLETE']
            int current_num = (int)actual_list_tests[(String)test_data.test_name]
            actual_list_tests[(String)test_data.test_name] = current_num + 1
        }

        assert list_tests['management'] == actual_list_tests['management']
    }
}
