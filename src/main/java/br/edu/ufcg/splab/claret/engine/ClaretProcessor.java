package br.edu.ufcg.splab.claret.engine;

import br.edu.ufcg.splab.claret.model.*;

import java.util.*;

public class ClaretProcessor {

    public static List<TestCase> extractTestCases(UseCase uc) {
        return extractTestCases("System", uc, CoverageCriteria.GT);
    }

    public static List<TestCase> extractTestCases(UseCase uc, CoverageCriteria criteria) {
        return extractTestCases("System", uc, criteria);
    }

    public static List<TestCase> extractTestCases(String systemName, UseCase uc, CoverageCriteria criteria) {
        List<TestCase> allPaths = generateAllPaths(systemName, uc);

        if (criteria == CoverageCriteria.BASIC_ONLY) {
            List<TestCase> basicList = new ArrayList<>();
            if (!allPaths.isEmpty()) {
                basicList.add(allPaths.get(0));
            }
            return basicList;
        }

        if (criteria == CoverageCriteria.ALL_BRANCHES) {
            List<TestCase> branches = new ArrayList<>();
            for (int i = 1; i < allPaths.size(); i++) {
                branches.add(allPaths.get(i));
            }
            return branches;
        }

        if (criteria == CoverageCriteria.COMPLETE) {
            return allPaths;
        }

        if (criteria == CoverageCriteria.GTP) {
            return selectTransitionPairCoverage(allPaths);
        }

        if (criteria == CoverageCriteria.ART) {
            return selectAdaptiveRandomTesting(allPaths);
        }

        // Default GT (Greedy Transition Coverage)
        return selectGreedyTransitionCoverage(allPaths);
    }

    private static List<TestCase> generateAllPaths(String systemName, UseCase uc) {
        List<TestCase> testCases = new ArrayList<>();

        // 1. Basic Flow (Happy Path)
        TestCase tcBasic = new TestCase(
            "TC1",
            systemName,
            uc.getName(),
            uc.getVersion(),
            "Basic Flow (Happy Path)",
            uc.getPreCondition(),
            uc.getPostCondition()
        );
        populateTestSteps(uc.getBasicFlow(), tcBasic.getSteps());
        testCases.add(tcBasic);

        int tcCounter = 2;

        // 2. Alternative Flows
        for (AlternativeFlow af : uc.getAlternatives()) {
            TestCase tcAlt = new TestCase(
                "TC" + (tcCounter++),
                systemName,
                uc.getName(),
                uc.getVersion(),
                "Alternative Flow " + af.getId() + ": " + af.getDescription(),
                uc.getPreCondition(),
                uc.getPostCondition()
            );

            int deviationStepIndex = findBasicStepIndexReferencingAf(uc.getBasicFlow(), af.getId());

            List<Step> fullAltPath = new ArrayList<>();
            if (deviationStepIndex >= 0) {
                for (int i = 0; i < deviationStepIndex; i++) {
                    fullAltPath.add(uc.getBasicFlow().get(i));
                }
            }

            fullAltPath.addAll(af.getSteps());

            Integer returnStepNum = getReturnStepNumber(af.getSteps());
            if (returnStepNum != null) {
                int returnIndex = findBasicStepIndexByNumber(uc.getBasicFlow(), returnStepNum);
                if (returnIndex >= 0) {
                    for (int i = returnIndex; i < uc.getBasicFlow().size(); i++) {
                        fullAltPath.add(uc.getBasicFlow().get(i));
                    }
                }
            }

            populateTestSteps(fullAltPath, tcAlt.getSteps());
            testCases.add(tcAlt);
        }

        // 3. Exception Flows
        for (ExceptionFlow ef : uc.getExceptions()) {
            TestCase tcExc = new TestCase(
                "TC" + (tcCounter++),
                systemName,
                uc.getName(),
                uc.getVersion(),
                "Exception Flow " + ef.getId() + ": " + ef.getDescription(),
                uc.getPreCondition(),
                "Flow interrupted due to exception / error"
            );

            int deviationStepIndex = findBasicStepIndexReferencingEf(uc.getBasicFlow(), ef.getId());

            List<Step> fullExcPath = new ArrayList<>();
            if (deviationStepIndex >= 0) {
                for (int i = 0; i < deviationStepIndex; i++) {
                    fullExcPath.add(uc.getBasicFlow().get(i));
                }
            }

            fullExcPath.addAll(ef.getSteps());

            populateTestSteps(fullExcPath, tcExc.getSteps());
            testCases.add(tcExc);
        }

        return testCases;
    }

    private static List<TestCase> selectGreedyTransitionCoverage(List<TestCase> allPaths) {
        if (allPaths.size() <= 1) return allPaths;

        Set<String> uncovered = new HashSet<>();
        for (TestCase tc : allPaths) {
            for (TestStep st : tc.getSteps()) {
                uncovered.add(st.getAction() + "->" + st.getExpectedResult());
            }
        }

        List<TestCase> selected = new ArrayList<>();
        Set<String> covered = new HashSet<>();

        if (!allPaths.isEmpty()) {
            TestCase basic = allPaths.get(0);
            selected.add(basic);
            for (TestStep st : basic.getSteps()) {
                covered.add(st.getAction() + "->" + st.getExpectedResult());
            }
        }

        for (int i = 1; i < allPaths.size(); i++) {
            TestCase tc = allPaths.get(i);
            boolean addsNewCoverage = false;
            for (TestStep st : tc.getSteps()) {
                String key = st.getAction() + "->" + st.getExpectedResult();
                if (!covered.contains(key)) {
                    addsNewCoverage = true;
                    break;
                }
            }
            if (addsNewCoverage) {
                selected.add(tc);
                for (TestStep st : tc.getSteps()) {
                    covered.add(st.getAction() + "->" + st.getExpectedResult());
                }
            }
        }

        return selected;
    }

    private static List<TestCase> selectTransitionPairCoverage(List<TestCase> allPaths) {
        if (allPaths.size() <= 1) return allPaths;

        List<TestCase> selected = new ArrayList<>();
        Set<String> coveredPairs = new HashSet<>();

        for (TestCase tc : allPaths) {
            boolean addsNewPair = false;
            List<TestStep> steps = tc.getSteps();
            for (int i = 0; i < steps.size() - 1; i++) {
                String pair = steps.get(i).getAction() + "=>" + steps.get(i + 1).getAction();
                if (!coveredPairs.contains(pair)) {
                    addsNewPair = true;
                }
            }
            if (addsNewPair || selected.isEmpty()) {
                selected.add(tc);
                for (int i = 0; i < steps.size() - 1; i++) {
                    String pair = steps.get(i).getAction() + "=>" + steps.get(i + 1).getAction();
                    coveredPairs.add(pair);
                }
            }
        }

        return selected;
    }

    private static List<TestCase> selectAdaptiveRandomTesting(List<TestCase> allPaths) {
        if (allPaths.size() <= 2) return allPaths;

        List<TestCase> selected = new ArrayList<>();
        selected.add(allPaths.get(0));

        List<TestCase> remaining = new ArrayList<>(allPaths.subList(1, allPaths.size()));

        while (!remaining.isEmpty() && selected.size() < Math.max(2, allPaths.size() * 3 / 4)) {
            TestCase bestCandidate = null;
            double maxMinDistance = -1.0;

            for (TestCase cand : remaining) {
                double minDistance = Double.MAX_VALUE;
                for (TestCase sel : selected) {
                    double dist = jaccardDistance(cand, sel);
                    if (dist < minDistance) {
                        minDistance = dist;
                    }
                }
                if (minDistance > maxMinDistance) {
                    maxMinDistance = minDistance;
                    bestCandidate = cand;
                }
            }

            if (bestCandidate != null) {
                selected.add(bestCandidate);
                remaining.remove(bestCandidate);
            } else {
                break;
            }
        }

        return selected;
    }

    private static double jaccardDistance(TestCase tc1, TestCase tc2) {
        Set<String> set1 = new HashSet<>();
        for (TestStep st : tc1.getSteps()) set1.add(st.getAction());

        Set<String> set2 = new HashSet<>();
        for (TestStep st : tc2.getSteps()) set2.add(st.getAction());

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        if (union.isEmpty()) return 0.0;
        double similarity = (double) intersection.size() / union.size();
        return 1.0 - similarity;
    }

    private static void populateTestSteps(List<Step> steps, List<TestStep> targetTestSteps) {
        int i = 0;
        int stepNum = 1;

        while (i < steps.size()) {
            Step current = steps.get(i);

            if ("system".equalsIgnoreCase(current.getActor())) {
                targetTestSteps.add(new TestStep(
                    stepNum++,
                    "system",
                    "SYSTEM: " + current.getAction(),
                    current.getAction()
                ));
                i++;
            } else {
                String expected = "SYSTEM processes action successfully";
                if (i + 1 < steps.size() && "system".equalsIgnoreCase(steps.get(i + 1).getActor())) {
                    expected = "SYSTEM " + steps.get(i + 1).getAction();
                    targetTestSteps.add(new TestStep(
                        stepNum++,
                        current.getActor(),
                        current.getAction(),
                        expected
                    ));
                    i += 2;
                } else {
                    targetTestSteps.add(new TestStep(
                        stepNum++,
                        current.getActor(),
                        current.getAction(),
                        expected
                    ));
                    i++;
                }
            }
        }
    }

    private static int findBasicStepIndexReferencingAf(List<Step> basicFlow, int afId) {
        for (int i = 0; i < basicFlow.size(); i++) {
            if (basicFlow.get(i).getAlternativeFlowIds().contains(afId)) {
                return i;
            }
        }
        return -1;
    }

    private static int findBasicStepIndexReferencingEf(List<Step> basicFlow, int efId) {
        for (int i = 0; i < basicFlow.size(); i++) {
            if (basicFlow.get(i).getExceptionFlowIds().contains(efId)) {
                return i;
            }
        }
        return -1;
    }

    private static Integer getReturnStepNumber(List<Step> steps) {
        for (int i = steps.size() - 1; i >= 0; i--) {
            if (steps.get(i).getBasicFlowStepReturn() != null) {
                return steps.get(i).getBasicFlowStepReturn();
            }
        }
        return null;
    }

    private static int findBasicStepIndexByNumber(List<Step> basicFlow, int stepNumber) {
        for (int i = 0; i < basicFlow.size(); i++) {
            if (basicFlow.get(i).getNumber() == stepNumber) {
                return i;
            }
        }
        return -1;
    }
}
