package com.cisco.mercury.test.restapi

import com.sun.javafx.runtime.SystemProperties
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

    static def username
    static def password
    static def port
    static def ip_address

    def setupSpec() {
        username = System.getProperty("username") ?: "admin"
        password = System.getProperty("password") ?: ""
        port = System.getProperty("port") ?: "8445"
        ip_address = System.getProperty("ip_address") ?: ""
        client = new RESTClient("https://" + ip_address + ":" + port + "/v1/")
        client.handler.failure = client.handler.success

        client.auth.basic(username, password)
        client.ignoreSSLIssues()
    }

    def cleanupSpec() {

    }

}
