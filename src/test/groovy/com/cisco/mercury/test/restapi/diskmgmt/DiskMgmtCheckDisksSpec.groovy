package com.cisco.mercury.test.restapi.diskmgmt

import groovy.json.JsonSlurper
import org.junit.Ignore
import spock.lang.Shared
import spock.lang.Unroll

class DiskMgmtCheckDisksSpec extends DiskMgmtBaseSpec {

    @Shared String test_uuid = null
    def setup() {}

    def cleanup() {
        if( test_uuid != null ) {
            waitForTest(test_uuid)
        }
    }

    @Unroll
    def "Test DiskMgmt create check-disks for role #role test"() {
        given: "A valid Mercury REST API Client on a C-Series PoD"
        assert client != null

        when: "The diskmgmt create command is POSTed for a check-disks test"
        def resp = createCheckDisks(role)

        then: "A check-checks test is created"
        assert resp.status == CREATED

        and: "Returns a UUID"
        def diskmgmt_request = getDiskMgmtRequest(resp)
        def test_uuid = diskmgmt_request['uuid']
        assert test_uuid != null

        when: "The test completes"
        waitForTest(test_uuid)

        then: "The results can be obtained"
        def show_resp = getShowResult(test_uuid)
        assert show_resp.status == OK

        and: "The UUID can be verified"
        def show_uuid = show_resp.responseData['uuid']
        assert show_uuid == test_uuid

        and: "The test job status can be verified"
        def jsonSlurper = new JsonSlurper()
        def job_detail = jsonSlurper.parseText((String)show_resp.responseData['diskmgmt_result'])

        assert job_detail.status == "PROCESSED"
        assert job_detail.message.size() == 1

        then: "The test details can be obtained"
        def test_details = parseDiskMgmtJsonStr(job_detail.message)

        and: "Overall status verified"
        assert test_details['Overall_Status'] == 'PASS'

        and: "The result structure verified"
        test_details['Result'].each { actual_test_name ->
            assert test_names.contains(actual_test_name.key)
        }

        then: "The results can be deleted"
        def del_resp = deleteDiskMgmtJob(test_uuid)

        and: "The delete was accepted"
        assert del_resp.status == OK

        //and: "The UUID for the test job is no longer available"

        where:
        test_names = [
                "add_as_spares_disks_results_list",
                "bad_disks_results_list",
                "fcfg_disks_results_list",
                "raid_results_list",
                "rbld_disks_results_list",
                "spare_disks_results_list"]
        role << ['all', 'control', 'compute', 'management']
    }

    @Unroll
    def "Test DiskMgmt create check-disks for role #role with a single server test"() {
        given: "A valid Mercury REST API Client on a C-Series PoD"
        assert client != null

        when: "The diskmgmt create command is POSTed for a check-disks test"
        def target_server = server[0]
        def resp = createCheckDisks(role, target_server)

        then: "A check-checks test is created"
        assert resp.status == CREATED

        and: "Returns a UUID"
        def diskmgmt_request = getDiskMgmtRequest(resp)
        def test_uuid = diskmgmt_request['uuid']
        assert test_uuid != null

        when: "The test completes"
        waitForTest(test_uuid)

        then: "The results can be obtained"
        def show_resp = getShowResult(test_uuid)
        assert show_resp.status == OK

        and: "The UUID can be verified"
        def show_uuid = show_resp.responseData['uuid']
        assert show_uuid == test_uuid

        and: "The test job status can be verified"
        def jsonSlurper = new JsonSlurper()
        def job_detail = jsonSlurper.parseText((String)show_resp.responseData['diskmgmt_result'])

        assert job_detail.status == "PROCESSED"
        assert job_detail.message.size() == 1

        then: "The test details can be obtained"
        def test_details = parseDiskMgmtJsonStr(job_detail.message)

        and: "Overall status verified"
        assert test_details['Overall_Status'] == 'PASS'

        and: "The result structure verified"
        test_details['Result'].each { actual_test ->
            assert test_names.contains(actual_test.key)
        }

        and: "Any results only contains the target server"
        test_details['Result'].each { actual_test ->
            def dm_list_result = actual_test.value
            if( dm_list_result.size() > 0) {
                assert dm_list_result.size() == 1
                assert dm_list_result[0].host == target_server
            }
        }

        then: "The results can be deleted"
        def del_resp = deleteDiskMgmtJob(test_uuid)

        and: "The delete was accepted"
        assert del_resp.status == OK

        //and: "The UUID for the test job is no longer available"

        where:
        test_names = [
                "add_as_spares_disks_results_list",
                "bad_disks_results_list",
                "fcfg_disks_results_list",
                "raid_results_list",
                "rbld_disks_results_list",
                "spare_disks_results_list"]
        role << ['control', 'compute']
        server << [ getNodeNames("control"), getNodeNames("compute")]
    }


    //def "Test DiskMgmt create check-disks for role #role with multi-server test"() {
    //    given: "A valid Mercury REST API Client on a C-Series PoD"
    //    assert client != null
    //}
}
