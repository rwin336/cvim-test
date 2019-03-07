package com.cisco.mercury.test.restapi

import com.sun.javafx.runtime.SystemProperties
import groovy.json.JsonSlurper
import groovy.json.internal.LazyMap
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
    static def pod_type
    static def pod_series

    def setupSpec() {
        username = System.getProperty("username") ?: "admin"
        password = System.getProperty("password") ?: "836a277d8cc26ae85717"
        port = System.getProperty("port") ?: "8445"
        ip_address = System.getProperty("ip_address") ?: "172.29.87.100"
        pod_type = System.getProperty("pod_type") ?: "fullon"
        pod_series = System.getProperty("pod_series") ?: "C-Series"
        client = new RESTClient("https://" + ip_address + ":" + port + "/v1/")
        client.handler.failure = client.handler.success

        client.auth.basic(username, password)
        client.ignoreSSLIssues()
    }

    def cleanupSpec() {

    }

    def getNodes(String type='all') {
        def nodes_resp = client.get(path: 'nodes',
                requestContentType: ContentType.TEXT,
                headers: ['Content-Type': "application/json"])

        if(nodes_resp.status == OK) {
            def jsonSlurper = new JsonSlurper()
            def nodes = nodes_resp.responseData['nodes']
            if( type == "all") {
                return nodes
            } else {
                ArrayList target_nodes = new ArrayList()
                nodes.each { node ->
                    if( node.mtype.contains(type)) {
                        target_nodes.push(node)
                    }
                }
                return target_nodes
            }
        }
        return nodes_resp
    }

    List getNodeNames(String type="all") {
        List node_names = []
        def nodes = getNodes(type)
        nodes.each { node ->
            node_names.add((String)node.name)
        }
        return node_names
    }
}
