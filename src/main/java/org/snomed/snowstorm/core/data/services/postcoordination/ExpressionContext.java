package org.snomed.snowstorm.core.data.services.postcoordination;

import io.kaicode.elasticvc.api.BranchCriteria;
import io.kaicode.elasticvc.api.BranchService;
import io.kaicode.elasticvc.api.PathUtil;
import io.kaicode.elasticvc.api.VersionControlHelper;
import io.kaicode.elasticvc.domain.Branch;
import org.snomed.snowstorm.core.data.domain.Concept;
import org.snomed.snowstorm.core.data.services.ConceptService;
import org.snomed.snowstorm.core.data.services.ServiceException;
import org.snomed.snowstorm.core.util.TimerUtil;
import org.snomed.snowstorm.ecl.ECLQueryService;
import org.snomed.snowstorm.mrcm.MRCMService;
import org.snomed.snowstorm.mrcm.model.AttributeDomain;
import org.snomed.snowstorm.mrcm.model.MRCM;
import org.springframework.data.domain.PageRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ExpressionContext {

	private final VersionControlHelper versionControlHelper;
	private final MRCMService mrcmService;
	private final int maximumPostcoordinationLevel;
	private ConceptService conceptService;
	private final BranchService branchService;
	private org.snomed.snowstorm.ecl.ECLQueryService eclQueryService;
	private final String branch;
	private BranchCriteria branchCriteria;
	private final boolean useDependantReleaseBranchForMRCM;
	private BranchCriteria dependantReleaseBranchCriteria;
	private MRCM mrcm;
	private Set<AttributeDomain> mrcmUngroupedAttributes;
	private final DisplayTermsRequired displayTermsRequired;

	private TimerUtil timer;
	private Concept focusConcept;
	private String focusConceptId;
	private Set<String> ancestorsAndSelfOfFocusConcept;

	public ExpressionContext(String branch, boolean useDependantReleaseBranchForMRCM, int maximumPostcoordinationLevel,
			BranchService branchService, VersionControlHelper versionControlHelper,
			MRCMService mrcmService, DisplayTermsRequired displayTermsRequired) {

		this.branch = branch;
		this.useDependantReleaseBranchForMRCM = useDependantReleaseBranchForMRCM;
		this.maximumPostcoordinationLevel = maximumPostcoordinationLevel;
		this.branchService = branchService;
		this.versionControlHelper = versionControlHelper;
		this.mrcmService = mrcmService;
		this.displayTermsRequired = displayTermsRequired;
	}

	public void reset(TimerUtil timer) {
		this.timer = timer;
		focusConcept = null;
		focusConceptId = null;
		ancestorsAndSelfOfFocusConcept = null;
	}

	public BranchCriteria getBranchCriteria() {
		if (branchCriteria == null) {
			branchCriteria = versionControlHelper.getBranchCriteria(branch);
		}
		return branchCriteria;
	}

	public BranchCriteria getMRCMBranchCriteria() throws ServiceException {
		if (!useDependantReleaseBranchForMRCM) {
			return getBranchCriteria();
		}
		if (dependantReleaseBranchCriteria == null) {
			if (PathUtil.isRoot(branch)) {
				throw new ServiceException("Expressions can not be maintained in the root branch. Please create a child codesystem and use the working branch of that codesystem.");
			}
			Branch latest = branchService.findLatest(branch);
			dependantReleaseBranchCriteria = versionControlHelper.getBranchCriteriaAtTimepoint(PathUtil.getParentPath(branch), latest.getBase());
		}
		return dependantReleaseBranchCriteria;
	}

	public MRCM getBranchMRCM() throws ServiceException {
		if (mrcm == null) {
			mrcm = mrcmService.loadActiveMRCMFromCache(branch);
		}
		return mrcm;
	}

	public int getMaximumPostcoordinationLevel() {
		return maximumPostcoordinationLevel;
	}

	public Set<AttributeDomain> getMRCMUngroupedAttributes() throws ServiceException {
		if (mrcmUngroupedAttributes == null) {
			mrcmUngroupedAttributes = getBranchMRCM().getAttributeDomains().stream()
					.filter(Predicate.not(AttributeDomain::isGrouped))
					.collect(Collectors.toSet());
		}
		return mrcmUngroupedAttributes;
	}

	public Set<String> ecl(String ecl) throws ServiceException {
		return eclQueryService.selectConceptIds(ecl, getMRCMBranchCriteria(), false, PageRequest.of(0, 1000))
				.getContent().stream().map(Object::toString).collect(Collectors.toSet());
	}

	public Concept getFocusConceptWithActiveRelationships() throws ServiceException {
		if (focusConcept == null) {
			Map<String, Concept> conceptMap = new HashMap<>();
			conceptMap.put(focusConceptId, new Concept(focusConceptId));
			BranchCriteria mrcmBranchCriteria = getMRCMBranchCriteria();
			conceptService.joinRelationships(conceptMap, new HashMap<>(), null, mrcmBranchCriteria.getBranchPath(), mrcmBranchCriteria, getTimer(), true);
			focusConcept = conceptMap.get(focusConceptId);
		}
		return focusConcept;
	}

	public String getBranch() {
		return branch;
	}

	public TimerUtil getTimer() {
		return timer;
	}

	public Set<String> getAncestorsAndSelfOrFocusConcept() throws ServiceException {
		if (ancestorsAndSelfOfFocusConcept == null) {
			ancestorsAndSelfOfFocusConcept = getAncestorsAndSelf(focusConceptId);
		}
		return ancestorsAndSelfOfFocusConcept;
	}

	public Set<String> getAncestorsAndSelf(String conceptId) throws ServiceException {
		return eclQueryService.selectConceptIds(">>" + conceptId, getMRCMBranchCriteria(), false, PageRequest.of(0, 100)).getContent().stream()
				.map(Objects::toString).collect(Collectors.toSet());
	}

	public void setFocusConceptId(String focusConceptId) {
		this.focusConceptId = focusConceptId;
	}

	public void setConceptService(ConceptService conceptService) {
		this.conceptService = conceptService;
	}

	public ConceptService getConceptService() {
		return conceptService;
	}

	public void setEclQueryService(ECLQueryService eclQueryService) {
		this.eclQueryService = eclQueryService;
	}

	public ECLQueryService getEclQueryService() {
		return eclQueryService;
	}

	public DisplayTermsRequired getDisplayTermsCombination() {
		return displayTermsRequired;
	}
}
