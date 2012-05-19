package field.math.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import field.math.linalg.Vector3;
import field.math.util.PlaneRansac.Model;

public class PlaneCarver {

	List<Model> models = new ArrayList<Model>();

	public PlaneCarver(List<Vector3> points, float inside, int numInterations, int maxMembership, int minMembership) {
		LinkedHashSet<Vector3> pop = new LinkedHashSet<Vector3>();

		pop.addAll(points);

		while (true) {
			
			System.out.println(" ransac of <"+pop.size()+"> <"+maxMembership+">");
			PlaneRansac ran = new PlaneRansac(new ArrayList<Vector3>(pop), inside, numInterations, maxMembership);
			if (ran.bestModel != null) {
				System.out.println(" extracted <"+ran.bestModel.points.size()+">");
				models.add(ran.bestModel);
				pop.removeAll(ran.bestModel.points);
				maxMembership = pop.size();
			} else {
			}
			
			maxMembership = (int) Math.min(pop.size()*0.9f, Math.min(ran.maxMembership*1.1f,  (int) (maxMembership * 0.95f) - 1));

			if (maxMembership < minMembership)
				break;
		}
	}

}
