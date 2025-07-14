package net.autocrm.api.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * SFA 사용자 모델
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesUser {
    private String salesUserSeq;
    private String userId;
    private String userPw;
    private String userName;
    private String mobilePhone1;
    private String mobilePhone2;
    private String mobilePhone3;
    private String companyPhone1;
    private String companyPhone2;
    private String companyPhone3;
    private String homePhone1;
    private String homePhone2;
    private String homePhone3;
    private String fax1;
    private String fax2;
    private String fax3;
    private String eMail;
    private String homepage;
    private String companyNumber;
    private String dealerSeq;
    private String showroomSeq;
    private String teamSeq;
    private String gradeName;
    private String dutyName;
    private String authGroup;
    private String authSeq;
    private String dmName;
    private String dmShowroomName;
    private String juminNumber1;
    private String juminNumber2;
    private String zipCode;
    private String address1;
    private String address2;
    private String signFileName;
    private String salesUserPicture;
    private String marginUp;
    private String marginDown;
    private String marginLeft;
    private String marginRight;
    private String salesStatus;
    private LocalDateTime disable;
    private String createBy;
    private LocalDateTime createDate;
    private String updateBy;
    private LocalDateTime updateDate;
    private String windowSeq;
    private LocalDateTime enteringDate;
    private LocalDateTime retiringDate;
    private String oldUserPw;
    private LocalDateTime pwExpireDate;
    private String oldJuminNumber1;
    private String oldJuminNumber2;
    private String homeZipCode;
    private String homeAddress1;
    private String homeAddress2;
    private LocalDateTime birthday;
    private String solarLunar;
    private LocalDateTime weddingDate;
    private String married;
    private String shared;
    private String memo;
    private String otpSecretKey;
    private Integer pwCount;
    private String bonbu;
}

