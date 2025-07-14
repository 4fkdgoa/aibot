package net.autocrm.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.hibernate.annotations.ColumnTransformer;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class ClientDetails {

    /**
     * 일련번호
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "SEQ")
	private Integer seq;

	/**
	 * ID
	 */
	@Column(name = "ID", nullable = false, unique = true)
	private String id;

    /**
     * 명칭
     */
    @Column(name = "NM")
	private String nm;

	/**
	 * API Key
	 */
	@Column(name = "APIKEY", nullable = false, unique = true)
	@ColumnTransformer(
			read = "sfa.F_DEC(APIKEY)",
			write = "sfa.F_ENC(?)"
		)
    private String apikey;

	/**
	 * Client 구분
	 */
	@Column(name = "G", nullable = false)
	private String g;

    /**
     * URL
     */
    @Column(name = "URL")
	private String url;

    /**
     * S3 BUCKET
     */
    @Column(name = "S3BUCKET")
    private String s3bucket;

    /**
     * S3 ACCESS KEY
     */
    @Column(name = "S3ACCESSKEY")
	@ColumnTransformer(
			read = "sfa.F_DEC(S3ACCESSKEY)",
			write = "sfa.F_ENC(?)"
		)
    private String s3accesskey;

    /**
     * S3 SECRET KEY
     */
    @Column(name = "S3SECRETKEY")
	@ColumnTransformer(
			read = "sfa.F_DEC(S3SECRETKEY)",
			write = "sfa.F_ENC(?)"
		)
    private String s3secretkey;

    /**
     * S3 REGION
     */
    @Column(name = "S3REGION")
    private String s3region;

    /**
     * WDMS ID
     */
    @Column(name = "WDMSID")
    private String wdmsid;

    /**
     * WDMS SECRET
     */
    @Column(name = "WDMSSECRET")
	@ColumnTransformer(
			read = "sfa.F_DEC(WDMSSECRET)",
			write = "sfa.F_ENC(?)"
		)
    private String wdmssecret;
}
