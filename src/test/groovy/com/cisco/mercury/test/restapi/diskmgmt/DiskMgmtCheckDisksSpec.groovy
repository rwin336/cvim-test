package com.cisco.mercury.test.restapi.diskmgmt

import groovy.json.JsonSlurper
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
    def "Test DiskMgmt create check-disks for each #role test"() {
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

}
