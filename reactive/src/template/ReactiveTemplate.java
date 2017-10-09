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


	private int numActions;
	private Agent myAgent;
	private int numberCities;
	private double [] maxNeighbor;
	private City [] bestNeighbor;

	private double d(double[] V, double[] V_) {
		// Return dinf(V, V_)
		double d = 0;
		for(int i = 0; i < V.length; ++i) {
			if(Math.abs(V[i] - V_[i]) > d)
				d = Math.abs(V[i] - V_[i]);
		}
		System.out.println(d);
		return d;
	}

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		// Here we should implement the strategy computing.
		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);


		this.numActions = 0;
		this.myAgent = agent;
		Vehicle myVehicle = agent.vehicles().get(0);

		this.numberCities = topology.size();


		double [ ] [ ] Q = new double [ numberCities ] [ numberCities  + 1]; // The last one for the task, the firsts for the simple moves
		for (int i = 0; i<numberCities; i++){
			for (int j = 0; j<= numberCities; j++){
				Q[i][j] = 0;
			}
		}

		double [] V = new double [numberCities];
		for (int i = 0; i< numberCities;i++) V[i] = 0;

		this.bestNeighbor = new City [numberCities];

		this.maxNeighbor = new double [numberCities];

		double [] previous_V;


		do{

			previous_V = V;
			V = new double [numberCities];


			for(City origin:topology){
				// Let us see what happens when we take the task we see
				double maximum;
				double averageRewardFromHere = 0;
				for (City destination:topology){
					if (origin != destination) averageRewardFromHere += td.probability(origin, destination) *
							(td.reward(origin, destination) - (origin.distanceTo(destination) * myVehicle.costPerKm()) +
									(discount * previous_V[destination.id]));

				}
				// We store it in the last column of our city row in Q this average of a task reward
				Q[origin.id][numberCities] = averageRewardFromHere;
				// And suppose it is more valuable to take the task
				maximum = averageRewardFromHere;


				// Otherwise we just go to a neighbour
				double max_neighbor = -Double.MAX_VALUE;
				City best_neighbor = origin;

				for (City neighbor : origin){
					double rewardForThisNeighbor = (discount * previous_V[neighbor.id]) - origin.distanceTo(neighbor)*myVehicle.costPerKm();
					Q[origin.id][neighbor.id] = rewardForThisNeighbor;
					if (rewardForThisNeighbor > maximum){
						maximum = rewardForThisNeighbor;
					}
					if (rewardForThisNeighbor > max_neighbor){
						max_neighbor = rewardForThisNeighbor;
						best_neighbor = neighbor;

					}
				}

				V[origin.id] = maximum;
				maxNeighbor[origin.id] = max_neighbor;
				bestNeighbor[origin.id] = best_neighbor;



			}




		} while(d(V,previous_V)>0.0000000000001);





	}


	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;
		City currentCity = vehicle.getCurrentCity();

		// Currently everything is random but this is where we must use our strategy, when do we chose to pick up a task and where do we go if we do not
		if ((availableTask == null) ||
				(availableTask.weight>vehicle.capacity()) ||
				((availableTask.reward - (currentCity.distanceTo(availableTask.deliveryCity) * vehicle.costPerKm())) > maxNeighbor[currentCity.id]))  { // Or the task is not enough attractive compared to its best neighbour

			// Then chose the best neighbor
			action = new Move(bestNeighbor[currentCity.id]);
		} else {
			// Otherwise we pick up the task
			action = new Pickup(availableTask);
		}

		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;

		return action;
	}

}
