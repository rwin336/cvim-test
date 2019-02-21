package com.cisco.mercury.test.restapi.cloudsanity

import groovyx.net.http.ContentType

class CloudSanityEmptyListSpecification extends CloudSanityBaseSpecification {


    def "Test main cloud-sanity endpoint returns an empty list when the database is empty"() {
        given: "A valid Mercury REST API Client"
        assert client != null

        when: "Performing GET on main cloudsanity endpoint when database is empty"
        def resp = client.get(path: 'cloudsanity',
                requestContentType: ContentType.JSON,
                headers: ['Content-Type': "application/json"] )

        then: "The response is OK and an empty list is returned"
        assert resp.status == OK
        assert resp.responseData.size() == 0
    }

    def "Test cloudsanity/list/?test_name=all endpoint returns an empty list when the database is empty"() {
        given: "A valid Mercury REST API Client"
        assert client != null

        when: "Performing GET on cloudsanity/list/?test_name=all endpoint when database is empty"
        def resp = client.get(path: 'cloudsanity/list/',
                requestContentType: ContentType.JSON,
                query: [test_name: 'all'],
                headers: ['Content-Type': "application/json"] )

        then: "The response is OK and an empty list is returned"
        assert resp.status == OK
        assert resp.responseData.size() == 0
    }


    def "Test cloudsanity/list/?test_name=control endpoint returns an empty list when the database is empty"() {
        given: "A valid Mercury REST API Client"
        assert client != null

        when: "Performing GET on cloudsanity/list/?test_name=control endpoint when database is empty"
        def resp = client.get(path: 'cloudsanity/list/',
                requestContentType: ContentType.JSON,
                query: [test_name: 'control'],
                headers: ['Content-Type': "application/json"] )

        then: "The response is OK and an empty list is returned"
        assert resp.status == OK
        assert resp.responseData.size() == 0
    }


    def "Test cloudsanity/list/?test_name=compute endpoint returns an empty list when the database is empty"() {
        given: "A valid Mercury REST API Client"
        assert client != null

        when: "Performing GET on cloudsanity/list/?test_name=compute endpoint when database is empty"
        def resp = client.get(path: 'cloudsanity/list/',
                requestContentType: ContentType.JSON,
                query: [test_name: 'compute'],
                headers: ['Content-Type': "application/json"] )

        then: "The response is OK and an empty list is returned"
        assert resp.status == OK
        assert resp.responseData.size() == 0
    }

    def "Test cloudsanity/list/?test_name=cephmon endpoint returns an empty list when the database is empty"() {
        given: "A valid Mercury REST API Client"
        assert client != null

        when: "Performing GET on cloudsanity/list/?test_name=cephmon endpoint when database is empty"
        def resp = client.get(path: 'cloudsanity/list/',
                requestContentType: ContentType.JSON,
                query: [test_name: 'cephmon'],
                headers: ['Content-Type': "application/json"] )

        then: "The response is OK and an empty list is returned"
        assert resp.status == OK
        assert resp.responseData.size() == 0
    }

    def "Test cloudsanity/list/?test_name=cephosd endpoint returns an empty list when the database is empty"() {
        given: "A valid Mercury REST API Client"
        assert client != null

        when: "Performing GET on cloudsanity/list/?test_name=cephosd endpoint when database is empty"
        def resp = client.get(path: 'cloudsanity/list/',
                requestContentType: ContentType.JSON,
                query: [test_name: 'cephosd'],
                headers: ['Content-Type': "application/json"] )

        then: "The response is OK and an empty list is returned"
        assert resp.status == OK
        assert resp.responseData.size() == 0
    }

    def "Test cloudsanity/list/?test_name=management endpoint returns an empty list when the database is empty"() {
        given: "A valid Mercury REST API Client"
        assert client != null

        when: "Performing GET on cloudsanity/list/?test_name=management endpoint when database is empty"
        def resp = client.get(path: 'cloudsanity/list/',
                requestContentType: ContentType.JSON,
                query: [test_name: 'management'],
                headers: ['Content-Type': "application/json"] )

        then: "The response is OK and an empty list is returned"
        assert resp.status == OK
        assert resp.responseData.size() == 0
    }

    def "Test cloudsanity/list/?test_name=bogus endpoint returns 404 not found"() {
        given: "A valid Mercury REST API Client"
        assert client != null

        when: "Performing GET on cloudsanity/list/?test_name=bogus endpoint when database is empty"
        def resp = client.get(path: 'cloudsanity/list/',
                requestContentType: ContentType.JSON,
                query: [test_name: 'bogus'],
                headers: ['Content-Type': "application/json"] )

        then: "The response is NOT_FOUND"
        assert resp.status == NOT_FOUND
    }

}
