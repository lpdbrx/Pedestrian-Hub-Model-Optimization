package SimulatedAnnealing._MinFunction;


import SimulatedAnnealing.Factories.SAProblem;
import SimulatedAnnealing.Others.ControlledGestionLists;
import SimulatedAnnealing.Others.Utils;

import java.util.*;

public class MinFunction extends SAProblem {

    private final static ArrayList<Double> range = createRange();

    private double x;

    private final static double start = -2;
    private final static double end = 2;
    private final static double step = 0.001;

    //to avoid to IndexOutOfRangeError
    private final static int intervalForLocalSearch = Math.min(20, (int)((end-step)/(step*2)-1));

    public MinFunction(double x){
        this.x = x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getX() {
        return x;
    }

    //liste des paramètres défénissant entièrement le problème : ici une seule variable
    public static ArrayList<Double> problemInit() {
        //last index of range is the current solution
        Random rand = new Random();
        int index = rand.nextInt(range.size());
        ArrayList<Double> minFunction = new ArrayList<>();
        minFunction.add(range.get(index));
        return minFunction;
    }

    private static ArrayList<Double> createRange() {
        ArrayList<Double> range = new ArrayList<>();
        double i = start;
        while (i <= end) {
            range.add(i);
            i += step;
        }
        return range;
    }

    @Override
    public ArrayList<Object> getParams() {
        ArrayList<Double> list = new ArrayList<>();
        list.add(x);
        return new ArrayList<>(list);
    }

    @Override
    public ControlledGestionLists CGInit(int length) {
        if(length > range.size())
            System.err.println("Length of A too big");
        ArrayList<SAProblem> X = new ArrayList<>(length);
        ArrayList<Double> Y = new ArrayList<>(length);
        double newX;
        for (int i = 0; i < length; i++) {
            newX = getRandomX();
            MinFunction pb = new MinFunction(newX);
            X.add(pb);
            Y.add(getObjectiveFunction(newX));
        }
        ControlledGestionLists.reorderCGs(X, Y);
        return new ControlledGestionLists(X,Y);
    }

    @Override
    public void printSolution(String s) {
        System.out.println(s);
        System.out.println("For x = " + x + ", y = " + this.objectiveFunction());
    }

    @Override
    public SAProblem transformSolutionLSA() {
        double w = Utils.randomProba();
        double nextX;
        if(w<0.75) {
            do {
                nextX = getRandomX();
            } while(x==nextX);
        }
        else {
            //local search
            int currIndex = range.indexOf(x);
            int minIndex = Math.max(0, currIndex-intervalForLocalSearch);
            int maxIndex = Math.min(range.size()-2, currIndex+intervalForLocalSearch);
            int nextIndex;
            do{
                nextIndex = Utils.randomInt(minIndex, maxIndex);
                nextX = range.get(nextIndex);
            } while(x==nextX);
        }
        return new MinFunction(nextX);
    }

    @Override
    public SAProblem transformSolutionDSA(ArrayList<SAProblem> CGListX, int n) {

        double w = Utils.randomProba();
        double nextX;
        if(w<0.75) {
            //Uniform distribution
            do {
                nextX = getRandomX();
            } while(x==nextX);
        }
        else {
            //Controlled generation
            nextX = getNextX(CGListX, n);
            //boolean unavailable = isUnavailable(CGListX, currX, nextX);
            //while(nextX < start || nextX > end || unavailable) {
            //    nextX = getNextX(CGListX, n);
            //    unavailable = isUnavailable(CGListX, currX, nextX);
            //}
        }
        return new MinFunction(nextX);
    }

    private boolean isUnavailable(ArrayList<SAProblem> CGListX, double currX, double nextX) {
        boolean unavailable = (nextX == currX);
        for (SAProblem pb : CGListX) {
            double xInCG = (double) pb.getParams().get(pb.getParams().size()-1);
            if(nextX == xInCG) {
                unavailable = true;
            }
        }
        return unavailable;
    }

    private double getNextX(ArrayList<SAProblem> CGListX, int n) {
        double nextX;

        //get n random elements in CGList
        List<SAProblem> CGcopy = new ArrayList<>(CGListX);
        CGcopy.remove(0);
        Collections.shuffle(CGcopy);
        CGcopy = CGcopy.subList(0, n);

        //compute nextX
        double G = ((MinFunction) (CGListX.get(0))).getX();
        for (int i = 0; i < n-1 ; i++) {
            G+= ((MinFunction) (CGcopy.get(i))).getX();
        }
        G = G / ((double) n);
        nextX = 2 * G - ((MinFunction) (CGcopy.get(n-1))).getX();

        //to avoid infinite loop when nextX already exist in CGListX when n = 1 OR delete while in transformF° CG part
        //double epsilon = 0.0000000000000001*Utils.randomInt(0,10);
        return nextX;//+epsilon;

    }

    private double getRandomX() {
        Random rand = new Random();
        int index = rand.nextInt(range.size()-2);
        return range.get(index);
    }


    @Override
    public double objectiveFunction() {
        return Math.log(0.1*Math.sin(10*x) + 0.01*Math.pow(x, 4) - 0.1 *Math.pow(x,2) +1)+1+0.7*x*x;
    }

    public double getObjectiveFunction(Double x) {
        return Math.log(0.1*Math.sin(10*x) + 0.01*Math.pow(x, 4) - 0.1 *Math.pow(x,2) +1)+1+0.7*x*x;
    }

    public void writeDataCurrX(String title, double currX) {
        String data = "";
        data += Utils.format(currX, 23);

        Utils.dataToTxt(title, data, true);
    }



}
