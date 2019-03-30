package SimulatedAnnealing;

import SimulatedAnnealing.Factories.SAProblemsAbstractFactory;
import SimulatedAnnealing.Others.ControlledGestionLists;
import SimulatedAnnealing.Others.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ContinuousProblem extends SAProblem {

    private List<Double> X;

    private static int dim;
    private static ArrayList<Double> starts;
    private static ArrayList<Double> ends;
    private static ArrayList<Double>  intervalsForLocalSearch = new ArrayList<>(dim);


    // Initialisation

    public ContinuousProblem(List<Double> param) {
        this.X = param;
    }

    /**
     *
     * initialize all continuous pb variables : @dim, @starts, @ends, @intervalsForLocalSearch
     *
     * @param dimension : dimension of the continuous problem
     * @param startRanges : start of the range for each axis (size = dimension)
     * @param endRanges : end of the range for each axis (size = dimension)
     * @return initial values for each axis
     */
    public static ArrayList<Double> problemInit(int dimension, List<Double> startRanges, List<Double> endRanges) {
        if(startRanges.size() != dimension || endRanges.size() != dimension) {
            System.err.println("Parameter dimension should be equal to the size of both lists");
        }
        ContinuousProblem.dim = dimension;
        ContinuousProblem.starts = new ArrayList<>(startRanges);
        ContinuousProblem.ends = new ArrayList<>(endRanges);

        ArrayList<Double> initialValues = new ArrayList<>();
        double Xi;
        for (int i = 0; i < dimension; i++) {
            double start = startRanges.get(i);
            double end = endRanges.get(i);
            Xi = Utils.randomDouble(start, end);
            initialValues.add(Xi);
            //initialize intervalFor
            ContinuousProblem.intervalsForLocalSearch.add((end-start)/20.0);
        }

        return initialValues;
    }



    // Getters

    /**
     *
     * @return dimension of the problem
     */
    public int getDimension(){
        return dim;
    }

    /**
     *
     * @return the value of current Xi for each axis
     */
    public ArrayList<Object> getXs() {
        return new ArrayList<>(X);
    }

    /**
     *
     * @param i : the axis
     * @return the current value of axis i
     */
    private double getXi(int i) {
        return X.get(i);
    }

    /**
     *
     * @param start : first possible value
     * @param end : last possible value
     * @return a random double in [start, end]
     */
    protected double getRandomXi(double start, double end) {
        return Utils.randomDouble(start, end);
    }

    /**
     *
     * @return start of range for each axis (size = dim)
     */
    protected ArrayList<Double> getStarts() {
        return starts;
    }

    /**
     *
     * @return end of range for each axis (size = dim)
     */
    protected ArrayList<Double> getEnds() {
        return ends;
    }



    // Algorithms

    /**
     *
     * @param i : the axis
     * @param X : the value around which we search nextXi
     * @return next value of axis i that shoud be next added to nextX
     */
    private double localSearchAroundX(int i, double X) {
        double nextXi;
        double min = Math.max(starts.get(i), X - intervalsForLocalSearch.get(i));
        double max = Math.min(X + intervalsForLocalSearch.get(i), ends.get(i));
        do{
            nextXi = Utils.randomDouble(min, max);
        } while(getXi(i) == nextXi);
        System.out.println("local best search");
        return nextXi;
    }

    /**
     *
     * Fills next values for each axis in nextX directly
     * @param nextX : next values for each
     */
    private void uniformDistribution(ArrayList<Double> nextX) {
        double nextXi;
        for (int i = 0; i < dim ; i++) {
            do {
                nextXi = getRandomXi(ends.get(i), starts.get(i));
            } while (getXi(i) == nextXi);
            nextX.add(nextXi);
        }
    }

    /**
     *
     * @param CGListXs : the CGList for each axis
     * @param i : the axis
     * @return nextXi that shoud be next added to nextX (uses the local search if CG is stuck)
     */
    private double controlledGeneration(ArrayList<ContinuousProblem> CGListXs, int i) {
        boolean check, stuck = false;
        int counter = 0;
        double nextXi;
        do {
            nextXi = getNextXiForCG(CGListXs, i);

            check = nextXi >= starts.get(i) && nextXi <= ends.get(i);
            //on est bloqué
            if(counter>=7*(dim+1)) {
                check = true;
                stuck = true;
            }
            counter++;
        } while(!check);

        // When we are stuck in CGList (occurs when 2 mini locals are near of the range border).
        // Example for n = 1 --> nextX = 2 * xMin - xMax => OUT OF RANGE
        // Local best search
        if(stuck) {
            double bestXi = ((ContinuousProblem)CGListXs.get(0)).getXi(i);
            nextXi = localSearchAroundX(i, bestXi);
        }
        return nextXi;
    }

    /**
     *
     * @param CGListXs : the CGList for each axis
     * @param i : the axis
     * @return nextXi that shoud be next added to nextX iff it's not stuck
     */
    private double getNextXiForCG(ArrayList<ContinuousProblem> CGListXs, int i) {
        //CGListXs is equivalent to an ArrayList<ArrayList<Double>>

        //get n random elements in CGList
        List<ContinuousProblem> CGcopy = new ArrayList<>(CGListXs);
        CGcopy.remove(0);
        Collections.shuffle(CGcopy);
        CGcopy = CGcopy.subList(0, dim);

        //compute nextX
        double G = CGListXs.get(0).getXi(i);
        for (int j = 0; j < dim-1 ; j++) {
            ContinuousProblem pb = CGcopy.get(j);
            G+= pb.getXi(i);
        }
        G = G / ((double) dim);

        return 2 * G -  CGcopy.get(dim-1).getXi(i);
    }

    /**
     *
     * @return next values for each axis (size = dim)
     */
    public List<Object> transformSolutionLSA() {
        double w = Utils.randomProba();

        ArrayList<Double> nextX = new ArrayList<>(dim);
        if(w<0.75) {
            //Uniform distribution
            uniformDistribution(nextX);
        }
        else {
            //Local search
            for (int i = 0; i < dim ; i++) {
                double nextXi = localSearchAroundX(i, getXi(i));
                nextX.add(nextXi);
            }

        }
        return new ArrayList<>(nextX);
    }

    /**
     *
     * @return next values for each axis (size = dim)
     */
    private List<Object> transformSolutionDSA(ArrayList<ContinuousProblem> CGListXs) {
        double w = Utils.randomProba();

        ArrayList<Double> nextX = new ArrayList<>(dim);
        if(w<0.75) {
            //Uniform distribution
            uniformDistribution(nextX);
        }
        else {
            //Controlled generation
            for (int i = 0; i < dim; i++) {
                double nextXi = controlledGeneration(CGListXs, i);
                nextX.add(nextXi);
            }
        }
        return new ArrayList<>(nextX);
    }

    /**
     * @param temperature : initial temperature
     * @param final_CG_density : objective indicator of the density of the CGList
     * @param final_temp : final temperature
     * @param parameters : list that defines the problem
     * @param factory : type of problem used
     * @param title : title for data.txt file
     */
    static void optimizationDSA(double temperature, final double final_CG_density, double final_temp,
                                ArrayList<Object> parameters, SAProblemsAbstractFactory factory, String title) {
        //Create a random initial problem
        int CGListLength = /*5*(n+1);*/ 7*(dim+1);   /*10*(n+1);*/
        ContinuousProblem currentSolution = (ContinuousProblem) factory.createSAProblem(parameters);

        //Overwrite the previous .txt file
        long startTime = SAProblem.Helper.dataVisuForGraphs1(title, "                 BEST y|                 CURR y|              ACCEPT PB|  ACC-BEST|            TEMPERATURE|                DENSITY|ACTUAL#MARKOV|");
        SAProblem.Helper.dataVisuForDrawing1(title, currentSolution.getDimension());

        //Initialize list for CG
        ControlledGestionLists CGs = currentSolution.CGInit(CGListLength);
        ArrayList<ContinuousProblem> CGListX = CGs.getX();
        ArrayList<Double> CGListY = CGs.getY();
        ControlledGestionLists.reorderCGs(CGListX, CGListY);
        currentSolution.printSolution("The initial solution: ");

        //Keep track if the best solution
        ContinuousProblem bestSolution = (ContinuousProblem) factory.createSAProblem(currentSolution.getXs());

        //Outer loop parameters
        int loop_nb =0; //t
        String isAcceptedBest = "FF";
        boolean stopCriterion = false;

        //Cooling temp param
        double factor = 0.85, factorMin = 0.8, factorMax = 0.9;
        int prevIterInner = -1;

        //Loop until system has cooled
        while (!stopCriterion) {
            //Markov chain param
            boolean check = false;
            int iterInner = 0, Lt = 10*dim, maxIterInner =Lt;
            double acceptanceProba = 0;

            while (!check) {
                //Create the neighbour solution
                ContinuousProblem newSolution = (ContinuousProblem) factory.createSAProblem(currentSolution.transformSolutionDSA(CGListX));

                //Get energy of new solution and worst solution of CGListX
                double worstCGObjective = CGListY.get(CGListLength-1);
                double neighbourObjective = newSolution.objectiveFunction();

                //Decide if we should accept the neighbour (not only when it's better)
                double rand = Utils.randomProba();
                acceptanceProba = Utils.acceptanceProbability(worstCGObjective, neighbourObjective, temperature);

                //Solution is accepted
                if (acceptanceProba > rand) {
                    CGListX.set(CGListLength-1, newSolution);
                    CGListY.set(CGListLength-1, newSolution.objectiveFunction());
                    currentSolution = (ContinuousProblem) factory.createSAProblem(newSolution.getXs());
                    isAcceptedBest="TF";
                }

                //Update inner loop param
                iterInner++;
                check = (iterInner>=maxIterInner);


                //Keep track of the best solution found (=CGListX[0])
                if (neighbourObjective < bestSolution.objectiveFunction()) {
                    bestSolution = (ContinuousProblem) factory.createSAProblem(currentSolution.getXs());
                    isAcceptedBest="TT";
                    check = true;
                    //Control param CASE 1
                    factor = factorMax;
                }

                ControlledGestionLists.reorderCGs(CGListX, CGListY);


                //Compute length of the markov chains
                double fh = CGListY.get(CGListLength-1);
                double fl = CGListY.get(0);
                double F = 1-Math.exp(fl-fh);
                maxIterInner = Lt + (int)(Lt*F);
            }


            //New best sol has not been found in A && at least 2nd outer loop
            if(iterInner >= maxIterInner && prevIterInner != -1){
                //Control param CASE 2
                if(prevIterInner < maxIterInner) {
                    factor = factor - (factor - factorMin)*(1 - prevIterInner/(double)maxIterInner);
                }
                else {
                    factor = factorMax - (factorMax - factor)*maxIterInner/(double)prevIterInner;
                }
            }

            prevIterInner = iterInner;

            //Stopping criterion
            double CG_density = CGListY.get(CGListLength-1)-CGListY.get(0); //Math.abs(1 - CGListY.get(0)/CGListY.get(CGListLength-1));

            stopCriterion = temperature <= final_temp && CG_density <= final_CG_density;

            //Cool system
            temperature *= factor;
            loop_nb++;

            //BEST y, CURR y, ACCEPT PB, ACC-BEST Sol(TT/TF/FF), TEMPER°, DENSITY, MARKOV LENGTH
            currentSolution.writeDataDSA(title, bestSolution.objectiveFunction(), currentSolution.objectiveFunction(),
                    acceptanceProba, isAcceptedBest, temperature, CG_density, iterInner);

            SAProblem.Helper.dataVisuForDrawing2(title, currentSolution, bestSolution);

        }

        SAProblem.Helper.dataVisuForGraphs2(title, startTime, bestSolution, loop_nb, "Nbr d'outer loop: ");
    }



    // Abstract functions

    /**
     * This function is left abstract because it must use the objective
     * function which is problem defined so defined in a subclass.
     */
    public abstract ControlledGestionLists CGInit(int length);

    public abstract void printSolution(String s);

    public abstract double objectiveFunction();



    //DataVisualization functions for Continuous problems

    private void writeDataDSA(String title, double bestSolution, double currentSolution, double acceptanceProba, String isAccepted, double temp, double density, int markovLen) {
        //BEST y, CURR y, ACCEPT PB, ACC-BEST Sol(TT/TF/FF), TEMPER°, DENSITY, MARKOV LENGTH
        String data = SAProblem.Helper.getString(bestSolution, currentSolution, acceptanceProba, isAccepted, temp);
        data += Utils.format(density, 23);
        data += Utils.format(markovLen, 13);

        Utils.dataToTxt(title, data, true);
    }


}