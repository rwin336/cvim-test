package com.cisco.mercury.test.restapi

import spock.lang.*
import groovyx.net.http.*

class CvimRestApiSpecification extends Specification  {

    static OK = 200
    static CREATED = 201
    static BAD_REQUEST = 400
    static UNAUTHORIZED = 401
    static NOT_FOUND = 404
    static METHOD_NOT_ALLOWED = 405
    static CONFLICT = 409
    static INTERNAL_SERVER_ERROR = 500
    static NOT_IMPLEMENTED = 501
    static BAD_GATEWAY = 502
    static SERVICE_UNAVAILABLE = 503
    static GATEWAY_TIMEOUT = 504


    @Shared def client

    def setupSpec() {
        client = new RESTClient("https://172.29.87.100:8445/v1/")
        client.handler.failure = client.handler.success

        client.auth.basic('admin', 'a7766c144e758e72a6c1')
        client.ignoreSSLIssues()
    }

    def cleanupSpec() {

    }

}
