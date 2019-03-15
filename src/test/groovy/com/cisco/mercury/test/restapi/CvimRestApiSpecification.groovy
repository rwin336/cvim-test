package com.cisco.mercury.test.restapi

import org.apache.http.conn.HttpHostConnectException
import groovy.json.JsonSlurper
import groovyx.net.http.ContentType

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
    @Shared def test_hub
    static def username
    static def password
    static def port
    static def test_hub_port = "8554"
    static def ip_address
    static def pod_type
    static String pod_series

    static Map testinfo_status = ["RUNNING": "TestInfoRunning",
                                  "COMPLETE": "TestInfoComplete",
                                  "FAILED": "TestInfoFailed",
                                  "PENDING": "TestInfoPending",
                                  "SUCCESS": "TestInfoSuccess"]

    def setupSpec() {
        username = System.getProperty("username") ?: "admin"
        password = System.getProperty("password") ?: ""
        port = System.getProperty("port") ?: "8445"
        ip_address = System.getProperty("ip_address") ?: "172.29.87.100"
        pod_type = System.getProperty("pod_type") ?: "fullon"
        pod_series = (String)System.getProperty("pod_series") ?: "C-Series"

        try {
            test_hub = new RESTClient("http://" + ip_address + ":" + test_hub_port + "/v1/")
            test_hub.handler.failure = test_hub.handler.success
        } catch (HttpHostConnectException hhce) {
            return
        }

        def resp = test_hub.post(path: 'testinfo/create/',
                requestContentType: ContentType.JSON,
                headers: ['Content-Type': "application/json; charset=UTF-8"],
                body: ['testinfo_request':
                               ['uuid': '',
                                'subject':  'ui_config',
                                'command': 'create',
                                'role': 'mgmt',
                                'servers': '']])

        assert resp.status == CREATED

        def ui_config_data = resp.responseData
        def jsonSlurper = new JsonSlurper()
        def testinfo_request_resp = jsonSlurper.parseText(ui_config_data['testinfo_request'])
        def uuid = testinfo_request_resp['uuid']

        assert uuid != null

        waitForTestInfo(uuid=uuid)

        def ui_config_resp = test_hub.get(path: 'testinfo/show/',
                requestContentType: ContentType.TEXT,
                headers: ['Content-Type': "application/json"],
                query: ['uuid': uuid ])

        assert ui_config_resp.status == OK

        def ui_config = jsonSlurper.parseText(ui_config_resp.responseData["testinfo_result"])

        client = new RESTClient("https://" + ui_config['RestAPI-Url'] + "/v1/")
        client.handler.failure = client.handler.success
        client.auth.basic(ui_config['RestAPI-Username'], ui_config['RestAPI-Password'])
        client.ignoreSSLIssues()
    }

    def cleanupSpec() {}

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

    def getTestInfoStatus(uuid) {
        String status = "UNKNOWN"
        def show_resp = test_hub.get(path: 'testinfo/show/',
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

    def waitForTestInfo( String uuid, String status='COMPLETE', int poll=5, int timeout=600 ) {
        long start_time = System.currentTimeMillis()
        sleep(1 * 1000)
        long current_time = System.currentTimeMillis()

        String test_state = 'UNKNOWN'
        while(((current_time - start_time) / 1000) < timeout) {
            String current_status = getTestInfoStatus(uuid)

            if( current_status == testinfo_status[status] ||
                    current_status == testinfo_status["FAILED"] ||
                    current_status == "NOT_FOUND") {
                test_state = current_status
                break
            }
            sleep(poll * 1000)
            current_time = System.currentTimeMillis()
        }
        return test_state
    }


}
