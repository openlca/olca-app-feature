package org.openlca.app.results.regionalized;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openlca.app.components.ResultTypeSelection.EventHandler;
import org.openlca.app.util.CostResultDescriptor;
import org.openlca.core.matrix.LongPair;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.ContributionResultProvider;
import org.openlca.geo.RegionalizedResultProvider;
import org.openlca.geo.kml.LocationKml;

abstract class SelectionHandler implements EventHandler {

	private RegionalizedResultProvider result;

	SelectionHandler(RegionalizedResultProvider result) {
		this.result = result;
	}

	protected abstract void processResultData(List<LocationResult> results);

	@Override
	public void flowSelected(FlowDescriptor flow) {
		ContributionResultProvider<?> provider = result.result;
		Set<ProcessDescriptor> processes = provider.getProcessDescriptors();
		Map<Long, Double> results = new HashMap<>();
		for (ProcessDescriptor process : processes) {
			double v = provider.getSingleFlowResult(process, flow).value;
			results.put(process.getId(), v);
		}
		processResultData(getResultData(results));
	}

	@Override
	public void impactCategorySelected(ImpactCategoryDescriptor impact) {
		ContributionResultProvider<?> provider = result.result;
		Set<ProcessDescriptor> processes = provider.getProcessDescriptors();
		Map<Long, Double> results = new HashMap<>();
		for (ProcessDescriptor process : processes) {
			double v = provider.getSingleImpactResult(process, impact).value;
			results.put(process.getId(), v);
		}
		processResultData(getResultData(results));
	}

	@Override
	public void costResultSelected(CostResultDescriptor cost) {
		ContributionResultProvider<?> provider = result.result;
		Set<ProcessDescriptor> processes = provider.getProcessDescriptors();
		Map<Long, Double> results = new HashMap<>();
		for (ProcessDescriptor process : processes) {
			double v = provider.getSingleCostResult(process);
			v = cost.forAddedValue ? -v : v;
			results.put(process.getId(), v);
		}
		processResultData(getResultData(results));
	}

	private List<LocationResult> getResultData(Map<Long, Double> results) {
		List<LocationResult> list = new ArrayList<>();
		for (LocationKml data : result.kmlData) {
			LocationResult result = new LocationResult(data.kmlFeature,
					data.locationId);
			for (LongPair product : data.processProducts)
				result.amount += results.get(product.getFirst());
			list.add(result);
		}
		return list;
	}

}