/**
 * 
 */
package com.se421.cg.regression.test.cases;

import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ensoftcorp.atlas.core.script.Common;
import com.se421.cg.regression.Activator;
import com.se421.cg.regression.RegressionTest;

/**
 * Checks that Call Graph Construction results match expected results
 * 
 * Execute this class with Run As -> JUnit Plug-in Test
 * 
 * @author Ben Holland
 */
public class JavaCallGraphConstructionTests {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		RegressionTest.setUpBeforeClass(Activator.getDefault().getBundle(), "/projects/JavaCallGraphConstructionExamples.zip", "JavaCallGraphConstructionExamples");
	}

	@Before
	public void setUp() throws Exception {}

	@After
	public void tearDown() throws Exception {}

	/**
	 * This is really just a sanity check that the index exists and correct project was loaded
	 */
	@Test
	public void testExpectedFunctionExists() {
		if(Common.functions("test1").eval().nodes().isEmpty()) {
			fail("Unable to locate expected test function.");
		}
	}
	
}
