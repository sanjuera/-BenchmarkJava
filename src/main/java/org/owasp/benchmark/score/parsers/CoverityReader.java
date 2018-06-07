/**
* OWASP Benchmark Project
*
* This file is part of the Open Web Application Security Project (OWASP)
* Benchmark Project For details, please see
* <a href="https://www.owasp.org/index.php/Benchmark">https://www.owasp.org/index.php/Benchmark</a>.
*
* The OWASP Benchmark is free software: you can redistribute it and/or modify it under the terms
* of the GNU General Public License as published by the Free Software Foundation, version 2.
*
* The OWASP Benchmark is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
* even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details
*
* @author Dave Wichers <a href="https://www.aspectsecurity.com">Aspect Security</a>
* @created 2015
*/

package org.owasp.benchmark.score.parsers;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONArray;
import org.json.JSONObject;
import org.owasp.benchmark.score.BenchmarkScore;

public class CoverityReader extends Reader {

    public TestResults parse( File f ) throws Exception {
        String content = new String(Files.readAllBytes(Paths.get(f.getPath())));

        JSONObject obj = new JSONObject(content);
        int version = obj.getInt( "formatVersion" );

        String key = version > 1 ? "issues" : "mergedIssues";
        JSONArray arr = obj.getJSONArray(key);

        TestResults tr = new TestResults( "Coverity Code Advisor" ,true,TestResults.ToolType.SAST); // Coverity's tool is called Code Advisor or Code Advisor On Demand
        // Fixme: See if we can figure this out from some of the files they provide
        tr.setTime(f);
        
        for (int i = 0; i < arr.length(); i++)
        {
            TestCaseResult tcr = parseCoverityFinding( arr.getJSONObject(i), version );
            if ( tcr != null ) {
                tr.put( tcr );
            }
        }
        
        return tr;
    }

    
    private TestCaseResult parseCoverityFinding(JSONObject finding, int version) {
        try {
            TestCaseResult tcr = new TestCaseResult();
            String filename = null;
            
            if ( version == 3 ) {
                filename = finding.getString("mainEventFilePathname");
                filename = filename.replaceAll( "\\\\", "/");
                filename = filename.substring( filename.lastIndexOf( '/' ) );
                if ( filename.contains( BenchmarkScore.BENCHMARKTESTNAME ) ) {
                    String testNumber = filename.substring( BenchmarkScore.BENCHMARKTESTNAME.length() + 1, filename.length() - 5 );
                    tcr.setNumber( Integer.parseInt( testNumber ) );
                    JSONObject props = finding.getJSONObject("checkerProperties");
                    String cweNumber = props.getString( "cweCategory" );
                    if ( cweNumber == null || cweNumber.equals ("none" ) ) {
                        return null;
                    }
                    int cwe = fixCWE( cweNumber );
                    tcr.setCWE( cwe );
                    tcr.setCategory( props.getString( "subcategoryShortDescription" ) );
                    tcr.setEvidence( props.getString( "subcategoryLongDescription" ) );                
                    // tcr.setConfidence( finding.getString( "impact" ));
                }
            } else if ( version == 2 ) {
                //
                // Version 2 as produced with Coverity client tools version 2018.3 and with
                // cov-format-errors --json-output-v2 somefile.json ...
                //
                return parseCoverityFindingV2(finding);
            // I believe this is for version == 1
            } else {
                filename = finding.getJSONArray("occurrences").getJSONObject(0).getString("mainEventFilePathname");
                filename = filename.replaceAll( "\\\\", "/");
                filename = filename.substring( filename.lastIndexOf( '/' ) );
                if ( filename.contains( BenchmarkScore.BENCHMARKTESTNAME ) ) {
                    String testNumber = filename.substring( BenchmarkScore.BENCHMARKTESTNAME.length() + 1, filename.length() - 5 );
                    tcr.setNumber( Integer.parseInt( testNumber ) );
                    if ( finding.isNull( "cweNumber" ) ) {
                        return null;
                    }
                    String cweNumber = finding.getString( "cweNumber" );
                    int cwe = fixCWE( cweNumber );
                    tcr.setCWE( cwe );
                    tcr.setCategory( finding.getString( "categoryDescription" ) );
                    tcr.setEvidence( finding.getString( "longDescription" ) );        
                    // tcr.setConfidence( finding.getString( "impact" ));
                }
            }
            return tcr;
        } catch (Exception e ) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * Parse one finding in a version 2 JSON file generated by Coverity.
     * <p>
     * One main difference with the other two versions is that there is no
     * CWE information in the file. So, the checker name is used to determine
     * which CWE to assign. The hardcoded list used by this method is based on
     * information in <code>issueTypes.json</code> in the output directory of
     * Coverity analysis.
     *
     * @param finding Coverity finding in JSON format that needs parsing
     * @return either a <code>TestCaseResult</code> object containing the finding details or <code>null</code>
     * if the finding is not relevant (not in a test case) or of an unknown type (that is: not part of the test
     * case definition)
     */
    private TestCaseResult parseCoverityFindingV2(JSONObject finding) {
        try {
            TestCaseResult tcr = new TestCaseResult();
            String filename = null;
            
            filename = finding.getString("mainEventFilePathname");
            filename = filename.replaceAll( "\\\\", "/");
            filename = filename.substring( filename.lastIndexOf( '/' ) );
            if ( filename.contains( BenchmarkScore.BENCHMARKTESTNAME ) ) {
                String testNumber = filename.substring( BenchmarkScore.BENCHMARKTESTNAME.length() + 1, filename.length() - 5 );
                tcr.setNumber( Integer.parseInt( testNumber ) );
                //
                // *** Warning: serious foefeling and cutting of corners ahead. ***
                //
                // This version of the Coverity JSON report does not have any hard
                // link to CWE numbers. However, it turns out that 'cov-analyze' generates
                // a file named 'issueTypes.json' and the combination of checkername and
                // subcategory from the analysis JSON file can be used as keys 'type' and
                // 'subtype' in 'issueTypes.json'.
                //
                // However, this does not work all the time (and the strings have to be used
                // case insensitive) E.g. in one file there can be "SQLI" while in the other
                // there is "SQL_INJECTION".
                //
                // The easiest implementation (that does not require an additional parameter pointing
                // to 'issueTypes.json') seems to use a couple of hardcoded checks instead of implementing
                // an elaborate lookup algorithm.
                //
                String checker_name = finding.getString("checkerName").toLowerCase();
                String subcategory = finding.getString("subcategory").toLowerCase();
                String cwe_string = "0";
                if(checker_name.equals("risky_crypto")) {
                    cwe_string = subcategory.equals("hashing") ? "328" : "327";
                } else if(checker_name.equals("path_manipulation")) {
                    cwe_string = "22";
                } else if(checker_name.equals("insecure_random")) {
                    cwe_string = "330";
                } else if(checker_name.equals("xpath_injection")) {
                    cwe_string = "94";
                } else if(checker_name.equals("os_cmd_injection")) {
                    cwe_string = "78";
                } else if(checker_name.equals("xss")) {
                    cwe_string = "79";
                } else if(checker_name.equals("sqli")) {
                    cwe_string = "89";
                } else if(checker_name.equals("ldap_injection")) {
                    cwe_string = "90";
                }
                int cwe = fixCWE(cwe_string);
                if(cwe <= 0) {
                    return null;
                }
                tcr.setCWE( cwe );
                tcr.setCategory(checker_name);
                tcr.setEvidence(subcategory);
                // tcr.setConfidence( finding.getString( "impact" ));
                return tcr;
            }
        } catch (Exception e ) {
            e.printStackTrace();
        }
        return null;
    }
    private int fixCWE( String cweNumber ) {
        int cwe = Integer.parseInt( cweNumber );
        if ( cwe == 94 ) cwe = 643;
        if ( cwe == 36 ) cwe = 22;
        if ( cwe == 23 ) cwe = 22;
        return cwe;
    }
    
}
