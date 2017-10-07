package template;

import java.util.Arrays;
import java.util.Random;
import java.util.List;
import java.lang.Math;

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class ReactiveTemplate implements ReactiveBehavior {

	private Random random;
	private double pPickup;
	private int numActions;
	private Agent myAgent;
	private int numberCities;
	private double [] maxNeighbor;
	private City [] bestNeighbor;
	private int [] idBestNeighbor;

	public double compute_cost(City origin, City destination) {
		List<City> path = origin.pathTo(destination);
		double result = 0;
		City last = origin;
		for (City next: path
			 ) {
			result = result + last.distanceTo(next);
			last = next;
		}

		return result;

	}

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		// Here we should implement the strategy computing.
		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);

		this.random = new Random();
		this.pPickup = discount;
		this.numActions = 0;
		this.myAgent = agent;
		Vehicle myVehicle = agent.vehicles().get(0);

		this.numberCities = topology.size();

		double [ ] [ ] prices = new double [ numberCities ] [ numberCities ];

		for (City origin:topology)
		{
			for (City destination:topology)
			{
				if (origin==destination){
					prices[origin.id][destination.id] = 0;
				} else {
					prices[origin.id][destination.id] = compute_cost(origin,destination);
				}
			}
		}


		double [ ] [ ] Q = new double [ numberCities ] [ numberCities  + 1];
		for (int i = 0; i<numberCities; i++){
			for (int j = 0; j<= numberCities; j++){
				Q[i][j] = 0;
			}
		}

		double [] V = new double [numberCities];
		for (int i = 0; i< numberCities;i++) V[i] = 0;

		this.bestNeighbor = new City [numberCities];
		this.idBestNeighbor = new int [numberCities];

		this.maxNeighbor = new double [numberCities];
		for (int i = 0; i< numberCities;i++) maxNeighbor[i] = 0;

		double [] previous_V;
		double last_difference = 100;
		double current_difference = 0;

		int test = 1;
		//while (Math.abs(last_difference-current_difference)0.001){
		while (test<10000000){
			test++;
			//System.out.println("On passe");
			previous_V = Arrays.copyOf(V,numberCities);

			for(City origin:topology){
				// Let us see what happens when we take the task we see
				double maximum = -8486454;
				double averageRewardFromHere = 0;
				for (City destination:topology){
					if (origin != destination){
						averageRewardFromHere += td.probability(origin,destination)*
								(td.reward(origin,destination)-origin.distanceTo(destination)*myVehicle.costPerKm()) +
								discount*previous_V[destination.id];

					}

				}
				// We store it in the last column of our city row in Q

				double max_neighbor = -8486454;
				City best_neighbor = origin;
				int idBest = 0;


				Q[origin.id][numberCities] = averageRewardFromHere;
				if (averageRewardFromHere > maximum){
					maximum = averageRewardFromHere;
				}

				// Otherwise we just go to a neighbour

				for (City neighbor : origin){
					double rewardForThisNeighbor = (discount * previous_V[neighbor.id]) - origin.distanceTo(neighbor)*myVehicle.costPerKm();
					Q[origin.id][neighbor.id] = rewardForThisNeighbor;
					if (rewardForThisNeighbor > maximum){
						maximum = rewardForThisNeighbor;
					}
					if (rewardForThisNeighbor > max_neighbor){
						max_neighbor = rewardForThisNeighbor;
						best_neighbor = neighbor;
						idBest = neighbor.id;
					}
				}

				V[origin.id] = maximum;
				maxNeighbor[origin.id] = max_neighbor;
				bestNeighbor[origin.id] = best_neighbor;
				idBestNeighbor[origin.id] = idBest;



			}


			last_difference = current_difference;
			current_difference = 0;
			for (int i = 0; i>numberCities; i++){
				current_difference += Math.abs(previous_V[i]-V[i]);
			}

		}

		for (int i=0; i<numberCities;i++){
			System.out.println(idBestNeighbor[i]);
		}




	}


	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;
		City currentCity = vehicle.getCurrentCity();

		// Currently everything is random but this is where we must use our strategy, when do we chose to pick up a task and where do we go if we do not
		if ((availableTask == null) || ((availableTask.reward - (currentCity.distanceTo(availableTask.deliveryCity) * vehicle.costPerKm())) > maxNeighbor[currentCity.id]))  { // Or the task is not enough attractive

			// Chose the best neighbor
			action = new Move(bestNeighbor[currentCity.id]);
		} else {
			action = new Pickup(availableTask);
		}

		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;

		return action;
	}
/*	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;
		// Currently everything is random but this is where we must use our strategy, when do we chose to pick up a task and where do we go if we do not
		if (availableTask == null || random.nextDouble() > pPickup) {
			City currentCity = vehicle.getCurrentCity();
			action = new Move(currentCity.randomNeighbor(random));
		} else {
			action = new Pickup(availableTask);
		}
		
		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;
		
		return action;
	}*/
}
