package org.openlca.app.editors.reports.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.openlca.app.App;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.editors.reports.model.ReportIndicatorResult.Contribution;
import org.openlca.app.editors.reports.model.ReportIndicatorResult.VariantResult;
import org.openlca.app.util.Numbers;
import org.openlca.core.database.CurrencyDao;
import org.openlca.core.math.ProjectCalculator;
import org.openlca.core.matrix.NwSetTable;
import org.openlca.core.model.Currency;
import org.openlca.core.model.Project;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.ContributionItem;
import org.openlca.core.results.ContributionSet;
import org.openlca.core.results.ImpactResult;
import org.openlca.core.results.ProjectResultProvider;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportCalculator implements Runnable {

	private Logger log = LoggerFactory.getLogger(getClass());

	private final Project project;
	private final Report report;

	public ReportCalculator(Project project, Report report) {
		this.project = project;
		this.report = report;
	}

	@Override
	public void run() {
		if (project == null || report == null)
			return;
		report.results.clear();
		report.addedValues.clear();
		report.netCosts.clear();
		if (project.getImpactMethodId() == null)
			return;
		ProjectResultProvider projectResult = calcProject(project);
		if (projectResult == null)
			return;
		appendResults(projectResult);
		appendCostResults(projectResult);
		if (project.getNwSetId() != null)
			appendNwFactors();
	}

	private ProjectResultProvider calcProject(Project project) {
		try {
			ProjectCalculator calculator = new ProjectCalculator(
					Cache.getMatrixCache(), App.getSolver());
			return calculator.solve(project, Cache.getEntityCache());
		} catch (Exception e) {
			log.error("Calculation of project failed", e);
			return null;
		}
	}

	private void appendNwFactors() {
		try {
			NwSetTable table = NwSetTable.build(Database.get(),
					project.getNwSetId());
			report.withNormalisation = table.hasNormalisationFactors();
			report.withWeighting = table.hasWeightingFactors();
			for (ReportIndicator indicator : report.indicators) {
				if (indicator.descriptor == null)
					continue;
				long categoryId = indicator.descriptor.getId();
				if (table.hasNormalisationFactors()) {
					double nf = table.getNormalisationFactor(categoryId);
					indicator.normalisationFactor = nf;
				}
				if (table.hasWeightingFactors()) {
					double wf = table.getWeightingFactor(categoryId);
					indicator.weightingFactor = wf;
				}
			}
		} catch (Exception e) {
			log.error("failed to load normalisation/weighting factors", e);
		}
	}

	private void appendResults(ProjectResultProvider result) {
		for (ImpactCategoryDescriptor impact : result.getImpactDescriptors()) {
			ReportIndicatorResult repResult = initReportResult(impact);
			if (repResult == null)
				continue; // should not add this indicator
			report.results.add(repResult);
			for (ProjectVariant variant : result.getVariants()) {
				VariantResult varResult = new VariantResult();
				repResult.variantResults.add(varResult);
				varResult.variant = variant.getName();
				ImpactResult impactResult = result.getTotalImpactResult(
						variant, impact);
				varResult.totalAmount = impactResult.value;
				ContributionSet<ProcessDescriptor> set = result
						.getResult(variant)
						.getProcessContributions(impact);
				appendProcessContributions(set, varResult);
			}
		}
	}

	private ReportIndicatorResult initReportResult(ImpactCategoryDescriptor impact) {
		for (ReportIndicator indicator : report.indicators) {
			if (!indicator.displayed)
				continue;
			if (Objects.equals(impact, indicator.descriptor))
				return new ReportIndicatorResult(indicator.id);
		}
		return null;
	}

	private void appendProcessContributions(
			ContributionSet<ProcessDescriptor> set, VariantResult varResult) {
		Contribution rest = new Contribution();
		varResult.contributions.add(rest);
		rest.rest = true;
		rest.processId = (long) -1;
		rest.amount = (double) 0;
		Set<Long> ids = getContributionProcessIds();
		Set<Long> foundIds = new TreeSet<>();
		for (ContributionItem<ProcessDescriptor> item : set.contributions) {
			if (item.item == null)
				continue;
			if (!ids.contains(item.item.getId()))
				rest.amount = rest.amount + item.amount;
			else {
				foundIds.add(item.item.getId());
				addContribution(varResult, item);
			}
		}
		addDefaultContributions(ids, foundIds, varResult);
	}

	private void addContribution(VariantResult varResult,
			ContributionItem<ProcessDescriptor> item) {
		Contribution con = new Contribution();
		varResult.contributions.add(con);
		con.amount = item.amount;
		con.rest = false;
		con.processId = item.item.getId();
	}

	private Set<Long> getContributionProcessIds() {
		Set<Long> ids = new TreeSet<>();
		for (ReportProcess process : report.processes) {
			if (process.descriptor == null)
				continue;
			ids.add(process.descriptor.getId());
		}
		return ids;
	}

	/**
	 * Add zero-contributions for processes that were not found in a variant
	 * result.
	 */
	private void addDefaultContributions(Set<Long> ids, Set<Long> foundIds,
			VariantResult varResult) {
		TreeSet<Long> notFound = new TreeSet<>(ids);
		notFound.removeAll(foundIds);
		for (long id : notFound) {
			Contribution con = new Contribution();
			varResult.contributions.add(con);
			con.amount = (double) 0;
			con.rest = false;
			con.processId = id;
		}
	}

	private void appendCostResults(ProjectResultProvider result) {
		if (result == null)
			return;
		String currency = getCurrency();
		for (ProjectVariant var : result.getVariants()) {
			double costs = result.getResult(var).getTotalCostResult();
			report.netCosts.add(cost(var, costs, currency));
			double addedValue = costs == 0 ? 0 : -costs;
			report.addedValues.add(cost(var, addedValue, currency));
		}
		Comparator<ReportCostResult> c = (r1, r2) -> {
			return Strings.compare(r1.variant, r2.variant);
		};
		Collections.sort(report.netCosts, c);
		Collections.sort(report.addedValues, c);
	}

	private ReportCostResult cost(ProjectVariant var, double val, String cu) {
		ReportCostResult r = new ReportCostResult();
		r.variant = var.getName();
		r.value = Numbers.decimalFormat(val, 2) + " " + cu;
		return r;
	}

	private String getCurrency() {
		try {
			CurrencyDao dao = new CurrencyDao(Database.get());
			Currency c = dao.getReferenceCurrency();
			if (c == null)
				return "?";
			return c.code != null ? c.code : c.getName();
		} catch (Exception e) {
			log.error("failed to load default currency", e);
			return "?";
		}
	}
}
