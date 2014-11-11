/**
 * Copyright (c) 2014 Lemur Consulting Ltd.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.flax.biosolr.ontology.api;

import org.apache.solr.client.solrj.beans.Field;

/**
 * @author Matt Pearce
 *
 */
public class Document {
	
	@Field("id")
	private String id;
	
	@Field("gid")
	private int gid;
	
	@Field("study_id")
	private int studyId;
	
	@Field("study")
	private int study;
	
	@Field("first_author")
	private String firstAuthor;
	
	@Field("publication")
	private String publication;
	
	@Field("title")
	private String title;
	
	@Field("snp")
	private String snp;
	
	@Field("disease_trait")
	private String diseaseTrait;
	
	@Field("p_value")
	private double pValue;
	
	@Field("efo_uri")
	private String efoUri;
	
	@Field("efo_uri_hash")
	private String efoUriHash;
	
	@Field("uri_key")
	private int uriKey;

	/**
	 * @return the gid
	 */
	public int getGid() {
		return gid;
	}

	/**
	 * @param gid the gid to set
	 */
	public void setGid(int gid) {
		this.gid = gid;
	}

	/**
	 * @return the studyId
	 */
	public int getStudyId() {
		return studyId;
	}

	/**
	 * @param studyId the studyId to set
	 */
	public void setStudyId(int studyId) {
		this.studyId = studyId;
	}

	/**
	 * @return the study
	 */
	public int getStudy() {
		return study;
	}

	/**
	 * @param study the study to set
	 */
	public void setStudy(int study) {
		this.study = study;
	}

	/**
	 * @return the firstAuthor
	 */
	public String getFirstAuthor() {
		return firstAuthor;
	}

	/**
	 * @param firstAuthor the firstAuthor to set
	 */
	public void setFirstAuthor(String firstAuthor) {
		this.firstAuthor = firstAuthor;
	}

	/**
	 * @return the publication
	 */
	public String getPublication() {
		return publication;
	}

	/**
	 * @param publication the publication to set
	 */
	public void setPublication(String publication) {
		this.publication = publication;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @return the snp
	 */
	public String getSnp() {
		return snp;
	}

	/**
	 * @param snp the snp to set
	 */
	public void setSnp(String snp) {
		this.snp = snp;
	}

	/**
	 * @return the diseaseTrait
	 */
	public String getDiseaseTrait() {
		return diseaseTrait;
	}

	/**
	 * @param diseaseTrait the diseaseTrait to set
	 */
	public void setDiseaseTrait(String diseaseTrait) {
		this.diseaseTrait = diseaseTrait;
	}

	/**
	 * @return the pValue
	 */
	public double getpValue() {
		return pValue;
	}

	/**
	 * @param pValue the pValue to set
	 */
	public void setpValue(double pValue) {
		this.pValue = pValue;
	}

	/**
	 * @return the efoUri
	 */
	public String getEfoUri() {
		return efoUri;
	}

	/**
	 * @param efoUri the efoUri to set
	 */
	public void setEfoUri(String efoUri) {
		this.efoUri = efoUri;
	}

	/**
	 * @return the efoUriHash
	 */
	public String getEfoUriHash() {
		return efoUriHash;
	}

	/**
	 * @param efoUriHash the efoUriHash to set
	 */
	public void setEfoUriHash(String efoUriHash) {
		this.efoUriHash = efoUriHash;
	}

	/**
	 * @return the uriKey
	 */
	public int getUriKey() {
		return uriKey;
	}

	/**
	 * @param uriKey the uriKey to set
	 */
	public void setUriKey(int uriKey) {
		this.uriKey = uriKey;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

}
