package fr.inria.astor.test.repair.evaluation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Ignore;
import org.junit.Test;

import fr.inria.astor.core.entities.OperatorInstance;
import fr.inria.astor.core.entities.ProgramVariant;
import fr.inria.astor.core.entities.TestCaseVariantValidationResult;
import fr.inria.astor.core.loop.AstorCoreEngine;
import fr.inria.astor.core.loop.population.PopulationConformation;
import fr.inria.astor.core.loop.spaces.ingredients.scopes.IngredientSpaceScope;
import fr.inria.astor.core.manipulation.MutationSupporter;
import fr.inria.astor.core.setup.ConfigurationProperties;
import fr.inria.astor.core.stats.Stats;
import fr.inria.astor.core.stats.PatchHunkStats;
import fr.inria.astor.core.stats.PatchStat;
import fr.inria.astor.core.stats.PatchStat.HunkStatEnum;
import fr.inria.astor.core.stats.PatchStat.PatchStatEnum;
import fr.inria.astor.util.CommandSummary;
import fr.inria.main.AstorOutputStatus;
import fr.inria.main.evolution.AstorMain;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtType;

/**
 * Test of Astor in mode jgenprog
 * 
 * @author Matias Martinez, matias.martinez@inria.fr
 *
 */
public class JGenProgTest extends BaseEvolutionaryTest {

	File out = null;

	public JGenProgTest() {
		out = new File(ConfigurationProperties.getProperty("workingDirectory"));
	}

	@Test
	public void testExample280CommandLine() throws Exception {
		AstorMain main1 = new AstorMain();
		String[] args = new String[] { "-bug280" };
		main1.main(args);
		validatePatchExistence(out + File.separator + "AstorMain-Math-issue-280/");
	}

	// TODO: THE PARENT OF A STATEMENT IS A CASE:
	// @Test
	public void testExample288CommandLine() throws Exception {
		AstorMain main1 = new AstorMain();
		String[] args = new String[] { "-bug288" };
		main1.main(args);
		validatePatchExistence(out + File.separator + "AstorMain-Math-issue-288/");
	}

	// @Test
	public void testExample340CommandLine() throws Exception {
		AstorMain main1 = new AstorMain();
		String[] args = new String[] { "-bug340" };
		main1.main(args);
		validatePatchExistence(out + File.separator + "Math-issue-340/");
	}

	// @Test
	public void testExample309CommandLine() throws Exception {
		AstorMain main1 = new AstorMain();
		String[] args = new String[] { "-bug309" };
		main1.main(args);
		validatePatchExistence(out + File.separator + "Math-issue-309/");
	}

	/**
	 * The fix is a replacement of an return statement
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMath85() throws Exception {
		AstorMain main1 = new AstorMain();
		String dep = new File("./examples/libs/junit-4.4.jar").getAbsolutePath();
		String[] args = new String[] { "-dependencies", dep, "-mode", "statement", "-failing",
				"org.apache.commons.math.distribution.NormalDistributionTest", "-location",
				new File("./examples/math_85").getAbsolutePath(), "-package", "org.apache.commons", "-srcjavafolder",
				"/src/java/", "-srctestfolder", "/src/test/", "-binjavafolder", "/target/classes", "-bintestfolder",
				"/target/test-classes", "-javacompliancelevel", "7", "-flthreshold", "0.5", "-stopfirst", "false",
				"-maxgen", "200", "-scope", "package", "-seed", "10" };
		System.out.println(Arrays.toString(args));
		main1.main(args);
		validatePatchExistence(out + File.separator + "AstorMain-math_85/");
	}

	/**
	 * Math 70 bug can be fixed by replacing a method invocation inside a return
	 * statement. + return solve(f, min, max); - return solve(min, max); One
	 * solution with local scope, another with package
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("rawtypes")
	@Test
	public void testMath70LocalSolution() throws Exception {
		AstorMain main1 = new AstorMain();
		String dep = new File("./examples/libs/junit-4.4.jar").getAbsolutePath();
		File out = new File(ConfigurationProperties.getProperty("workingDirectory"));
		int generations = 50;
		String[] args = commandMath70(dep, out, generations);
		CommandSummary cs = new CommandSummary(args);
		cs.command.put("-stopfirst", "false");

		System.out.println(Arrays.toString(cs.flat()));
		main1.execute(cs.flat());

		List<ProgramVariant> solutions = main1.getEngine().getSolutions();
		assertTrue(solutions.size() > 0);
		assertEquals(1, solutions.size());
		ProgramVariant variant = solutions.get(0);
		TestCaseVariantValidationResult validationResult = (TestCaseVariantValidationResult) variant
				.getValidationResult();

		assertTrue(validationResult.isRegressionExecuted());

		validatePatchExistence(out + File.separator + "AstorMain-math_70/", solutions.size());

		OperatorInstance mi = variant.getOperations().values().iterator().next().get(0);
		assertNotNull(mi);
		assertEquals(IngredientSpaceScope.LOCAL, mi.getIngredientScope());

		// mi.getIngredientScope()
		// Program variant ref to
		Collection<CtType<?>> affected = variant.getAffectedClasses();
		List<CtClass> progVariant = variant.getModifiedClasses();
		assertFalse(progVariant.isEmpty());

		for (CtType aff : affected) {
			CtType ctcProgVariant = returnByName(progVariant, (CtClass) aff);
			assertNotNull(ctcProgVariant);
			assertFalse(ctcProgVariant == aff);

			// Classes from affected set must be not equals to the program
			// variant cloned ctclasses,
			// due to these have include the changes applied for repairing the
			// bug.
			assertNotEquals(ctcProgVariant, aff);

			// Classes from affected set must be equals to the spoon model
			CtType ctspoon = returnByName(MutationSupporter.getFactory().Type().getAll(), (CtClass) aff);
			assertNotNull(ctcProgVariant);
			assertEquals(ctspoon, aff);
		}
	}

	/**
	 * Math 70 bug can be fixed by replacing a method invocation inside a return
	 * statement. + return solve(f, min, max); - return solve(min, max); One
	 * solution with local scope, another with package
	 * This test validates the stats via API and JSON
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("rawtypes")
	@Test
	public void testMath70LocalOutputs() throws Exception {
		AstorMain main1 = new AstorMain();
		String dep = new File("./examples/libs/junit-4.4.jar").getAbsolutePath();
		File out = new File(ConfigurationProperties.getProperty("workingDirectory"));
		int generations = 50;
		String[] args = commandMath70(dep, out, generations);
		CommandSummary cs = new CommandSummary(args);
		cs.command.put("-stopfirst", "false");

		System.out.println(Arrays.toString(cs.flat()));
		main1.execute(cs.flat());

		List<ProgramVariant> solutions = main1.getEngine().getSolutions();
		assertTrue(solutions.size() > 0);
		assertEquals(1, solutions.size());
		Stats stats = Stats.createStat();
		assertNotNull(stats);

		assertNotNull(stats.getStatsOfPatches());

		assertTrue(stats.getStatsOfPatches().size() > 0);

		String jsonpath = main1.getEngine().getProjectFacade().getProperties().getWorkingDirRoot() + File.separator
				+ ConfigurationProperties.getProperty("jsonoutputname") + ".json";
		File filejson = new File(jsonpath);
		assertTrue(filejson.exists());

		JSONParser parser = new JSONParser();

		Object obj = parser.parse(new FileReader(filejson));

		JSONObject jsonroot = (JSONObject) obj;

		// loop array
		JSONArray msg = (JSONArray) jsonroot.get("patches");
		assertEquals(1, msg.size());
		JSONObject pob = (JSONObject) msg.get(0);

		JSONArray hunks = (JSONArray) pob.get("patchhunks");
		assertEquals(1, hunks.size());
		JSONObject hunkob = (JSONObject) hunks.get(0);
		assertEquals("return solve(f, min, max)", hunkob.get(HunkStatEnum.PATCH_HUNK_CODE.name()));
		assertEquals("return solve(min, max)", hunkob.get(HunkStatEnum.ORIGINAL_CODE.name()));

		// Test API

		assertEquals(1, stats.getStatsOfPatches().size());

		PatchStat patchstats = stats.getStatsOfPatches().get(0);

		List<PatchHunkStats> hunksApi = (List<PatchHunkStats>) patchstats.getStats().get(PatchStatEnum.HUNKS);

		assertNotNull(hunksApi);

		PatchHunkStats hunkStats = hunksApi.get(0);

		assertNotNull(hunkStats);

		assertEquals("return solve(f, min, max)", hunkStats.getStats().get(HunkStatEnum.PATCH_HUNK_CODE));

		assertEquals("return solve(min, max)", hunkob.get(HunkStatEnum.ORIGINAL_CODE.name()));

	}

	public static String[] commandMath70(String dep, File out, int generations) {
		String[] args = new String[] { "-dependencies", dep, "-mode", "statement", "-failing",
				"org.apache.commons.math.analysis.solvers.BisectionSolverTest", "-location",
				new File("./examples/math_70").getAbsolutePath(), "-package", "org.apache.commons", "-srcjavafolder",
				"/src/java/", "-srctestfolder", "/src/test/", "-binjavafolder", "/target/classes", "-bintestfolder",
				"/target/test-classes", "-javacompliancelevel", "7", "-flthreshold", "0.5", "-out",
				out.getAbsolutePath(), "-scope", "local", "-seed", "10", "-maxgen", Integer.toString(generations),
				"-stopfirst", "true", "-maxtime", "100", "-loglevel", "INFO", "-parameters", "disablelog:false"

		};
		return args;
	}

	/**
	 * Return the ct type from the collection according tho the class passed as
	 * parameter.
	 * 
	 * @param classes
	 * @param target
	 * @return
	 */
	private CtType returnByName(Collection<?> classes, CtClass target) {

		for (Object ctClass : classes) {
			if (((CtType) ctClass).getSimpleName().equals(target.getSimpleName())) {
				return (CtType) ctClass;
			}
		}
		return null;
	}

	@Test
	public void testArguments() throws Exception {
		AstorMain main1 = new AstorMain();
		String dep = new File("./examples/libs/junit-4.4.jar").getAbsolutePath();
		File out = new File(ConfigurationProperties.getProperty("workingDirectory"));
		String[] args = new String[] { "-dependencies", dep, "-mode", "statement", "-failing",
				"org.apache.commons.math.analysis.solvers.BisectionSolverTest", "-location",
				new File("./examples/math_70").getAbsolutePath(), "-package", "org.apache.commons", "-srcjavafolder",
				"/src/java/", "-srctestfolder", "/src/test/", "-binjavafolder", "/target/classes", "-bintestfolder",
				"/target/test-classes", "-javacompliancelevel", "7", "-flthreshold", "0.5", "-out",
				out.getAbsolutePath(), "-scope", "package", "-seed", "10", "-stopfirst", "true", "-maxgen", "50",
				"-saveall", "false" };
		boolean correct = main1.processArguments(args);
		assertTrue(correct);

		String javahome = ConfigurationProperties.properties.getProperty("jvm4testexecution");

		assertNotNull(javahome);

		assertTrue(javahome.endsWith("bin"));
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void testMath70PackageSolutions() throws Exception {
		AstorMain main1 = new AstorMain();
		String dep = new File("./examples/libs/junit-4.4.jar").getAbsolutePath();
		File out = new File(ConfigurationProperties.getProperty("workingDirectory"));
		String[] args = new String[] { "-dependencies", dep, "-mode", "statement", "-failing",
				"org.apache.commons.math.analysis.solvers.BisectionSolverTest", "-location",
				new File("./examples/math_70").getAbsolutePath(), "-package", "org.apache.commons", "-srcjavafolder",
				"/src/java/", "-srctestfolder", "/src/test/", "-binjavafolder", "/target/classes", "-bintestfolder",
				"/target/test-classes", "-javacompliancelevel", "7", "-flthreshold", "0.5", "-out",
				out.getAbsolutePath(),
				//
				"-scope", "package", "-seed", "10", "-maxgen", "500", "-stopfirst", "false", // two
																								// solutions
				"-maxtime", "10", "-population", "1", "-reintroduce", PopulationConformation.PARENTS.toString()

		};
		System.out.println(Arrays.toString(args));
		main1.execute(args);

		List<ProgramVariant> solutions = main1.getEngine().getSolutions();
		assertTrue(solutions.size() >= 2);
		assertTrue(solutions.size() <= 3);
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void testMath70PackageSolutionsEvolving() throws Exception {
		AstorMain main1 = new AstorMain();
		String dep = new File("./examples/libs/junit-4.4.jar").getAbsolutePath();
		File out = new File(ConfigurationProperties.getProperty("workingDirectory"));
		String[] args = new String[] { "-dependencies", dep, "-mode", "statement", "-failing",
				"org.apache.commons.math.analysis.solvers.BisectionSolverTest", "-location",
				new File("./examples/math_70").getAbsolutePath(), "-package", "org.apache.commons", "-srcjavafolder",
				"/src/java/", "-srctestfolder", "/src/test/", "-binjavafolder", "/target/classes", "-bintestfolder",
				"/target/test-classes", "-javacompliancelevel", "7", "-flthreshold", "0.5", "-out",
				out.getAbsolutePath(),
				//
				"-scope", "package", "-seed", "10", "-maxgen", "500", "-stopfirst", "false", // two
																								// solutions
				"-maxtime", "10", "-population", "1", "-reintroduce", PopulationConformation.PARENTS.toString()
						+ File.pathSeparator + PopulationConformation.SOLUTIONS.toString()// Here
																							// we
																							// test.

		};
		System.out.println(Arrays.toString(args));
		main1.execute(args);

		List<ProgramVariant> solutions = main1.getEngine().getSolutions();
		assertTrue(solutions.size() > 3);
		boolean withMultiple = false;
		for (ProgramVariant programVariant : solutions) {
			System.out.println("-->" + programVariant.getOperations().values());
			withMultiple = withMultiple || programVariant.getOperations().values().size() >= 2;
		}
		assertTrue(withMultiple);
	}

	/**
	 * Testing injected bug at CharacterReader line 118, commit version 31be24.
	 * "org.jsoup.nodes.AttributesTest"+File.pathSeparator+"org.jsoup.nodes.DocumentTypeTest"
	 * +File.pathSeparator+"org.jsoup.nodes.NodeTest"+File.pathSeparator+"org.jsoup.parser.HtmlParserTest"
	 * 
	 * @throws Exception
	 */
	@Test
	@Ignore
	public void testJSoupParser31be24() throws Exception {
		String dep = new File("./examples/libs/junit-4.5.jar").getAbsolutePath();
		AstorMain main1 = new AstorMain();

		String[] args = new String[] { "-mode", "statement", "-location",
				new File("./examples/jsoup31be24").getAbsolutePath(), "-dependencies", dep,
				// The injected bug produces 4 failing cases in two files
				"-failing",
				"org.jsoup.parser.CharacterReaderTest" + File.pathSeparator + "org.jsoup.parser.HtmlParserTest",
				//
				"-package", "org.jsoup", "-javacompliancelevel", "7", "-stopfirst", "true",
				//
				"-flthreshold", "0.8", "-srcjavafolder", "/src/main/java/", "-srctestfolder", "/src/test/java/",
				"-binjavafolder", "/target/classes", "-bintestfolder", "/target/test-classes",
				//
				"-scope", "local", "-seed", "10", "-maxtime", "100", "-population", "1", "-maxgen", "250", "-saveall",
				"true" };
		main1.execute(args);

		List<ProgramVariant> solutions = main1.getEngine().getSolutions();
		assertEquals(1, solutions.size());
		// TODO: Problem printing CtThisAccess
		// pos += offset
		// time(sec)= 30
		// operation: ReplaceOp
		// location= org.jsoup.parser.CharacterReader
		// line= 118
		// original statement= pos -= offset
		// fixed statement= pos += offset
		// generation= 26
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void testMath50Remove() throws Exception {
		AstorMain main1 = new AstorMain();
		String dep = new File("./examples/libs/junit-4.8.2.jar").getAbsolutePath();
		File out = new File(ConfigurationProperties.getProperty("workingDirectory"));
		String[] args = new String[] { "-dependencies", dep, "-mode", "statement", "-failing",
				"org.apache.commons.math.analysis.solvers.RegulaFalsiSolverTest", "-location",
				new File("./examples/math_50").getAbsolutePath(), "-package", "org.apache.commons", "-srcjavafolder",
				"/src/main/java/", "-srctestfolder", "/src/test/java", "-binjavafolder", "/target/classes",
				"-bintestfolder", "/target/test-classes", "-javacompliancelevel", "5", "-flthreshold", "0.1", "-out",
				out.getAbsolutePath(), "-scope", "local", "-seed", "10", "-maxgen", "50", "-stopfirst", "true",
				"-maxtime", "100", "-ignoredtestcases", "org.apache.commons.math.util.FastMathTest" };
		System.out.println(Arrays.toString(args));
		main1.execute(args);

		List<ProgramVariant> solutions = main1.getEngine().getSolutions();
		assertTrue(solutions.size() > 0);
		assertEquals(1, solutions.size());
		ProgramVariant variant = solutions.get(0);

		TestCaseVariantValidationResult validationResult = (TestCaseVariantValidationResult) variant
				.getValidationResult();

		assertTrue(validationResult.isRegressionExecuted());

	}

	@SuppressWarnings("rawtypes")
	@Test
	@Ignore
	public void testMath76() throws Exception {
		AstorMain main1 = new AstorMain();
		String dep = new File("./examples/libs/junit-4.4.jar").getAbsolutePath();
		File out = new File(ConfigurationProperties.getProperty("workingDirectory"));
		String[] args = new String[] { "-dependencies", dep, "-mode", "statement", "-failing",
				"org.apache.commons.math.linear.SingularValueSolverTest", "-location",
				new File("./examples/math_76").getAbsolutePath(), "-package", "org.apache.commons", "-srcjavafolder",
				"/src/main/java/", "-srctestfolder", "/src/test/java", "-binjavafolder", "/target/classes",
				"-bintestfolder", "/target/test-classes", "-javacompliancelevel", "5", "-flthreshold", "0.5", "-out",
				out.getAbsolutePath(), "-scope", "local", "-seed", "6010", "-maxgen", "50", "-stopfirst", "true",
				"-maxtime", "2",

		};
		System.out.println(Arrays.toString(args));
		main1.execute(args);

		List<ProgramVariant> solutions = main1.getEngine().getSolutions();
		assertTrue(solutions.isEmpty());

	}

	@SuppressWarnings("rawtypes")
	@Test
	public void testMath74() throws Exception {
		AstorMain main1 = new AstorMain();
		String dep = new File("./examples/libs/junit-4.4.jar").getAbsolutePath();
		File out = new File(ConfigurationProperties.getProperty("workingDirectory"));
		String[] args = new String[] { "-dependencies", dep, "-mode", "statement", "-failing",
				"org.apache.commons.math.ode.nonstiff.AdamsMoultonIntegratorTest", "-location",
				new File("./examples/math_74").getAbsolutePath(), "-package", "org.apache.commons", "-srcjavafolder",
				"/src/main/java/", "-srctestfolder", "/src/test/java", "-binjavafolder", "/target/classes",
				"-bintestfolder", "/target/test-classes", "-javacompliancelevel", "5", "-flthreshold", "0.5", "-out",
				out.getAbsolutePath(), "-scope", "local", "-seed", "10", "-maxgen", "50", "-stopfirst", "true",
				"-maxtime", "2",

		};
		System.out.println(Arrays.toString(args));
		main1.execute(args);

		List<ProgramVariant> solutions = main1.getEngine().getSolutions();
		assertTrue(solutions.isEmpty());

	}

	@SuppressWarnings("rawtypes")
	@Test
	public void testMath106UndoException() throws Exception {
		AstorMain main1 = new AstorMain();
		String dep = new File("./examples/libs/commons-discovery-0.2.jar").getAbsolutePath() + File.pathSeparator
				+ new File("./examples/libs/commons-logging-1.0.4.jar").getAbsolutePath() + File.pathSeparator
				+ new File("./examples/libs/junit-3.8.2.jar").getAbsolutePath();
		File out = new File(ConfigurationProperties.getProperty("workingDirectory"));
		String[] args = new String[] { "-dependencies", dep, "-mode", "statement", "-failing",
				"org.apache.commons.math.fraction.FractionFormatTest", "-location",
				new File("./examples/math_106").getAbsolutePath(), "-package", "org.apache.commons", "-srcjavafolder",
				"/src/main/java/", "-srctestfolder", "/src/test/java", "-binjavafolder", "/target/classes",
				"-bintestfolder", "/target/test-classes", "-javacompliancelevel", "7", "-flthreshold", "0.5", "-out",
				out.getAbsolutePath(), "-scope", "local", "-seed", "6010", "-maxgen", "50", "-stopfirst", "true",
				"-maxtime", "30",

		};
		System.out.println(Arrays.toString(args));
		main1.execute(args);

		List<ProgramVariant> solutions = main1.getEngine().getSolutions();
		assertTrue(solutions.isEmpty());

	}

	@SuppressWarnings("rawtypes")
	@Test
	public void testMath70NotFailingAsArg() throws Exception {

		String originalFailing = "org.apache.commons.math.analysis.solvers.BisectionSolverTest";
		AstorMain main1 = new AstorMain();
		String dep = new File("./examples/libs/junit-4.4.jar").getAbsolutePath();
		File out = new File(ConfigurationProperties.getProperty("workingDirectory"));
		String[] args = new String[] { "-dependencies", dep, "-mode", "statement", //

				"-location", new File("./examples/math_70").getAbsolutePath(), "-package", "org.apache.commons",
				"-srcjavafolder", "/src/java/", "-srctestfolder", "/src/test/", "-binjavafolder", "/target/classes",
				"-bintestfolder", "/target/test-classes", "-javacompliancelevel", "7", "-flthreshold", "0.5", "-out",
				out.getAbsolutePath(), "-maxgen", "0",// Forced

		};
		System.out.println(Arrays.toString(args));
		main1.execute(args);
		List<String> deducedFailingTest = main1.getEngine().getProjectFacade().getProperties().getFailingTestCases();
		assertNotNull(deducedFailingTest);
		assertEquals(1, deducedFailingTest.size());
		log.debug("deduced: " + deducedFailingTest);
		assertTrue(deducedFailingTest.contains(originalFailing));
	}

	@Test
	public void testMath70Outputg() throws Exception {

		AstorMain main1 = new AstorMain();
		String dep = new File("./examples/libs/junit-4.4.jar").getAbsolutePath();
		File out = new File(ConfigurationProperties.getProperty("workingDirectory"));
		String[] args = new String[] { "-dependencies", dep, "-mode", "statement", //

				"-location", new File("./examples/math_70").getAbsolutePath(), "-package", "org.apache.commons",
				"-srcjavafolder", "/src/java/", "-srctestfolder", "/src/test/", "-binjavafolder", "/target/classes",
				"-bintestfolder", "/target/test-classes", "-javacompliancelevel", "7", "-flthreshold", "0.5", "-out",
				out.getAbsolutePath(), "-maxgen", "0", "-maxtime", "10", "-stopfirst", "true"

		};
		CommandSummary command = new CommandSummary(args);
		System.out.println(Arrays.toString(args));
		main1.execute(command.flat());
		AstorCoreEngine engine = main1.getEngine();

		assertEquals(AstorOutputStatus.MAX_GENERATION, engine.getOutputStatus());

		command.command.put("-maxgen", "10");
		command.command.put("-maxtime", "0");

		main1.execute(command.flat());
		engine = main1.getEngine();

		assertEquals(AstorOutputStatus.TIME_OUT, engine.getOutputStatus());

		command.command.put("-maxtime", "60");
		command.command.put("-maxgen", "100");
		main1.execute(command.flat());
		engine = main1.getEngine();

		assertEquals(AstorOutputStatus.STOP_BY_PATCH_FOUND, engine.getOutputStatus());

	}

	@Test
	public void testMath70DiffOfSolution() throws Exception {
		AstorMain main1 = new AstorMain();
		String dep = new File("./examples/libs/junit-4.4.jar").getAbsolutePath();
		File out = new File(ConfigurationProperties.getProperty("workingDirectory"));
		int generations = 50;
		String[] args = commandMath70(dep, out, generations);
		CommandSummary cs = new CommandSummary(args);
		cs.command.put("-stopfirst", "true");

		System.out.println(Arrays.toString(cs.flat()));
		main1.execute(cs.flat());

		List<ProgramVariant> solutions = main1.getEngine().getSolutions();
		assertTrue(solutions.size() > 0);
		assertEquals(1, solutions.size());
		ProgramVariant variant = solutions.get(0);
		assertFalse(variant.getPatchDiff().isEmpty());
		assertEquals(AstorOutputStatus.STOP_BY_PATCH_FOUND, main1.getEngine().getOutputStatus());

		String diff = variant.getPatchDiff();
		log.debug("Patch: " + diff);

	}

	@Test
	public void testMath70StopAtXVariantsSolution() throws Exception {
		AstorMain main1 = new AstorMain();
		String dep = new File("./examples/libs/junit-4.4.jar").getAbsolutePath();
		File out = new File(ConfigurationProperties.getProperty("workingDirectory"));
		String[] args = new String[] { "-dependencies", dep, "-mode", "statement", "-failing",
				"org.apache.commons.math.analysis.solvers.BisectionSolverTest", "-location",
				new File("./examples/math_70").getAbsolutePath(), "-package", "org.apache.commons", "-srcjavafolder",
				"/src/java/", "-srctestfolder", "/src/test/", "-binjavafolder", "/target/classes", "-bintestfolder",
				"/target/test-classes", "-javacompliancelevel", "7", "-flthreshold", "0.5", "-out",
				out.getAbsolutePath(),
				//
				"-scope", "package", "-seed", "10", "-maxgen", "500", "-stopfirst", "false", "-maxtime", "10",
				"-population", "1", "-reintroduce", PopulationConformation.PARENTS.toString()

		};
		System.out.println(Arrays.toString(args));
		CommandSummary command = new CommandSummary(args);

		command.command.put("-parameters", "maxnumbersolutions:2");
		main1.execute(command.flat());

		List<ProgramVariant> solutions = main1.getEngine().getSolutions();
		assertEquals(2, solutions.size());

		command.command.put("-parameters", "maxnumbersolutions:1");
		main1.execute(command.flat());

		solutions = main1.getEngine().getSolutions();
		assertEquals(1, solutions.size());

	}

	/**
	 * Math 70 bug can be fixed by replacing a method invocation inside a return
	 * statement. + return solve(f, min, max); - return solve(min, max); One
	 * solution with local scope, another with package
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("rawtypes")
	@Test
	public void testMath70LocalSolutionJUExLog() throws Exception {
		AstorMain main1 = new AstorMain();
		String dep = new File("./examples/libs/junit-4.4.jar").getAbsolutePath();
		File out = new File(ConfigurationProperties.getProperty("workingDirectory"));
		int generations = 200;
		String[] args = commandMath70(dep, out, generations);
		CommandSummary cs = new CommandSummary(args);
		cs.command.put("-stopfirst", "true");
		cs.command.put("-seed", "0");
		cs.command.put("-scope", "package");

		cs.command.put("-loglevel", "DEBUG");
		cs.command.put("-parameters", "disablelog:false");
		cs.append("-parameters", "testexecutorclass:JUnitExternalExecutor");

		assertEquals(4, cs.command.get("-parameters").split(File.pathSeparator).length);
		System.out.println(Arrays.toString(cs.flat()));
		main1.execute(cs.flat());

		List<ProgramVariant> solutions = main1.getEngine().getSolutions();
		assertTrue(solutions.size() > 0);
		assertEquals(1, solutions.size());

	}

	/**
	 * Math 70 bug can be fixed by replacing a method invocation inside a return
	 * statement. + return solve(f, min, max); - return solve(min, max); One
	 * solution with local scope, another with package
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("rawtypes")
	@Test
	public void testMath70LogFile() throws Exception {
		AstorMain main1 = new AstorMain();
		String dep = new File("./examples/libs/junit-4.4.jar").getAbsolutePath();
		File out = new File(ConfigurationProperties.getProperty("workingDirectory"));
		int generations = 200;
		String[] args = commandMath70(dep, out, generations);
		CommandSummary cs = new CommandSummary(args);
		cs.command.put("-stopfirst", "true");
		cs.command.put("-seed", "0");
		cs.command.put("-scope", "package");

		cs.command.put("-parameters", "disablelog:false");
		cs.append("-parameters", "testexecutorclass:JUnitExternalExecutor");
		File fileLog = File.createTempFile("logTest", ".log");
		cs.append("-parameters", "logfilepath:" + fileLog.getAbsolutePath());

		assertEquals(6, cs.command.get("-parameters").split(File.pathSeparator).length);
		System.out.println(Arrays.toString(cs.flat()));
		main1.execute(cs.flat());

		List<ProgramVariant> solutions = main1.getEngine().getSolutions();
		assertTrue(solutions.size() > 0);
		assertEquals(1, solutions.size());

		assertEquals(Level.INFO.toString(), ConfigurationProperties.getProperty("loglevel"));
		List<String> logInStringList = Files.readAllLines(fileLog.toPath());
		assertTrue(logInStringList.size() > 0);
		System.out.println(logInStringList.get(0));

		boolean allInfo = true;
		for (String lineLog : logInStringList) {
			if (lineLog.startsWith("[DEBUG]")) {
				allInfo = false;
				break;
			}
		}

		assertTrue("a debug line found", allInfo);

		// Reset log file
		assertTrue(fileLog.delete());
		assertTrue(fileLog.createNewFile());
		cs.command.put("-loglevel", Level.DEBUG.toString());
		main1.execute(cs.flat());

		logInStringList = Files.readAllLines(fileLog.toPath());
		assertTrue(logInStringList.size() > 0);
		boolean existDebugLog = false;
		for (String lineLog : logInStringList) {
			if (lineLog.startsWith("[DEBUG]")) {
				existDebugLog = true;
				break;
			}
		}
		assertTrue("Any debug line", existDebugLog);

	}

}
