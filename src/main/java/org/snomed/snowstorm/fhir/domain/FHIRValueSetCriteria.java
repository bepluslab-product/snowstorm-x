package org.snomed.snowstorm.fhir.domain;

import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.ValueSet;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.snomed.snowstorm.core.util.CollectionUtils.orEmpty;

public class FHIRValueSetCriteria {

	@Field(type = FieldType.Keyword)
	private String system;

	@Field(type = FieldType.Keyword)
	private String version;

	@Field(type = FieldType.Keyword)
	private List<ValueSet.ConceptReferenceComponent> concepts;

	private List<FHIRValueSetFilter> filter;

	private List<String> valueSet;

	public FHIRValueSetCriteria() {
	}

	public FHIRValueSetCriteria(ValueSet.ConceptSetComponent hapiCriteria) {
		system = hapiCriteria.getSystem();
		version = hapiCriteria.getVersion();
		concepts = hapiCriteria.getConcept();
		for (ValueSet.ConceptSetFilterComponent hapiFilter : hapiCriteria.getFilter()) {
			if (filter == null) {
				filter = new ArrayList<>();
			}
			filter.add(new FHIRValueSetFilter(hapiFilter));
		}
		valueSet = hapiCriteria.getValueSet().stream().map(CanonicalType::getValueAsString).collect(Collectors.toList());
	}

	public ValueSet.ConceptSetComponent getHapi() {
		ValueSet.ConceptSetComponent hapiConceptSet = new ValueSet.ConceptSetComponent();
		hapiConceptSet.setSystem(system);
		hapiConceptSet.setVersion(version);
		for (ValueSet.ConceptReferenceComponent concpet : orEmpty(concepts)) {
			hapiConceptSet.addConcept(concpet);
		}
		for (FHIRValueSetFilter filter : orEmpty(getFilter())) {
			hapiConceptSet.addFilter(filter.getHapi());
		}
		hapiConceptSet.setValueSet(orEmpty(valueSet).stream().map(CanonicalType::new).collect(Collectors.toList()));
		return hapiConceptSet;
	}

	public String getSystem() {
		return system;
	}

	public void setSystem(String system) {
		this.system = system;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public List<ValueSet.ConceptReferenceComponent> getConcepts() {
		return concepts;
	}

	public void setConcepts(List<ValueSet.ConceptReferenceComponent> concepts) {
		this.concepts = concepts;
	}

	public List<FHIRValueSetFilter> getFilter() {
		return filter;
	}

	public void setFilter(List<FHIRValueSetFilter> filter) {
		this.filter = filter;
	}
}
