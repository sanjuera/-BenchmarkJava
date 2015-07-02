/**
* OWASP WebGoat Benchmark Edition (WBE) v1.1
*
* This file is part of the Open Web Application Security Project (OWASP)
* WebGoat Benchmark Edition (WBE) project. For details, please see
* <a href="https://www.owasp.org/index.php/WBE">https://www.owasp.org/index.php/WBE</a>.
*
* The WBE is free software: you can redistribute it and/or modify it under the terms
* of the GNU General Public License as published by the Free Software Foundation, version 2.
*
* The WBE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
* even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details
*
* @author Nick Sanidas <a href="https://www.aspectsecurity.com">Aspect Security</a>
* @created 2015
*/

package org.owasp.webgoat.benchmark.testcode;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/BenchmarkTest01708")
public class BenchmarkTest01708 extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	
		String param = "";
		java.util.Enumeration<String> headerNames = request.getHeaderNames();
		if (headerNames.hasMoreElements()) {
			param = headerNames.nextElement(); // just grab first element
		}
		
		
		// Chain a bunch of propagators in sequence
		String a41133 = param; //assign
		StringBuilder b41133 = new StringBuilder(a41133);  // stick in stringbuilder
		b41133.append(" SafeStuff"); // append some safe content
		b41133.replace(b41133.length()-"Chars".length(),b41133.length(),"Chars"); //replace some of the end content
		java.util.HashMap<String,Object> map41133 = new java.util.HashMap<String,Object>();
		map41133.put("key41133", b41133.toString()); // put in a collection
		String c41133 = (String)map41133.get("key41133"); // get it back out
		String d41133 = c41133.substring(0,c41133.length()-1); // extract most of it
		String e41133 = new String( new sun.misc.BASE64Decoder().decodeBuffer( 
		    new sun.misc.BASE64Encoder().encode( d41133.getBytes() ) )); // B64 encode and decode it
		String f41133 = e41133.split(" ")[0]; // split it on a space
		org.owasp.webgoat.benchmark.helpers.ThingInterface thing = org.owasp.webgoat.benchmark.helpers.ThingFactory.createThing();
		String bar = thing.doSomething(f41133); // reflection
		
		
		java.io.FileOutputStream fos = new java.io.FileOutputStream(new java.io.File(org.owasp.webgoat.benchmark.helpers.Utils.testfileDir + bar));
	}
}