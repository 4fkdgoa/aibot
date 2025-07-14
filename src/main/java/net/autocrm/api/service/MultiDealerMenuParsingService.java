package net.autocrm.api.service;


import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.autocrm.api.model.MenuInfo;

@Service
@Slf4j
public class MultiDealerMenuParsingService {
    
    private final DealerConfigProperties dealerConfig;
    
    // 딜러별 메뉴 캐시: dealerId -> authSeq -> MenuInfo 리스트
    private Map<String, Map<String, List<MenuInfo>>> dealerMenuCache = new ConcurrentHashMap<>();
    private Map<String, LocalDateTime> lastParsedTimes = new ConcurrentHashMap<>();
    
    /**
     * 모든 딜러의 leftmenu.jsp 파싱
     */
    @PostConstruct
    public void parseAllDealerMenus() {
        Set<String> activeDealers = dealerConfig.getActiveDealers();
        
        log.info("활성화된 딜러 {}개의 메뉴 파싱 시작: {}", activeDealers.size(), activeDealers);
        
        for (String dealerId : activeDealers) {
            try {
                parseDealerMenus(dealerId);
            } catch (Exception e) {
                log.error("딜러 {} 메뉴 파싱 실패: {}", dealerId, e.getMessage(), e);
            }
        }
        
        log.info("전체 딜러 메뉴 파싱 완료");
    }
    
    /**
     * 특정 딜러의 메뉴 파싱
     */
    public void parseDealerMenus(String dealerId) {
        try {
            String leftMenuPath = dealerConfig.getLeftMenuPath(dealerId);
            
            if (!Files.exists(Paths.get(leftMenuPath))) {
                log.warn("딜러 {} leftmenu.jsp 파일이 존재하지 않습니다: {}", dealerId, leftMenuPath);
                return;
            }
            
            String jspContent = Files.readString(Paths.get(leftMenuPath), StandardCharsets.UTF_8);
            Map<String, List<MenuInfo>> dealerMenus = new HashMap<>();
            
            // 권한별 메뉴 파싱
            parseMenuByAuth(jspContent, "WebKeys.AUTH_SC", "SC", dealerMenus);
            parseMenuByAuth(jspContent, "WebKeys.AUTH_SL", "SL", dealerMenus);  
            parseMenuByAuth(jspContent, "WebKeys.AUTH_MS", "MS", dealerMenus);
            parseMenuByAuth(jspContent, "WebKeys.AUTH_GD", "GD", dealerMenus);
            parseMenuByAuth(jspContent, "WebKeys.AUTH_CEO", "CEO", dealerMenus);
            parseMenuByAuth(jspContent, "WebKeys.AUTH_SD", "SD", dealerMenus);
            parseMenuByAuth(jspContent, "WebKeys.AUTH_SH", "SH", dealerMenus);
            parseMenuByAuth(jspContent, "WebKeys.AUTH_CM", "CM", dealerMenus);
            parseMenuByAuth(jspContent, "WebKeys.AUTH_SCM", "SCM", dealerMenus);
            parseMenuByAuth(jspContent, "WebKeys.AUTH_SM", "SM", dealerMenus);
            parseMenuByAuth(jspContent, "WebKeys.AUTH_AM", "AM", dealerMenus);
            parseMenuByAuth(jspContent, "WebKeys.AUTH_RE", "RE", dealerMenus);
            parseMenuByAuth(jspContent, "WebKeys.AUTH_AD", "AD", dealerMenus);
            parseMenuByAuth(jspContent, "WebKeys.AUTH_BDC", "BDC", dealerMenus);
            parseMenuByAuth(jspContent, "WebKeys.AUTH_PIM", "PIM", dealerMenus);
            parseMenuByAuth(jspContent, "WebKeys.AUTH_PDC", "PDC", dealerMenus);
            parseMenuByAuth(jspContent, "WebKeys.AUTH_SA", "SA", dealerMenus);
            parseMenuByAuth(jspContent, "WebKeys.AUTH_SAD", "SAD", dealerMenus);
            parseMenuByAuth(jspContent, "WebKeys.AUTH_SCH", "SCH", dealerMenus);
            
            dealerMenuCache.put(dealerId, dealerMenus);
            lastParsedTimes.put(dealerId, LocalDateTime.now());
            
            int totalMenuCount = dealerMenus.values().stream()
                .mapToInt(menus -> menus.stream()
                    .mapToInt(menu -> 1 + menu.getChildren().size())
                    .sum())
                .sum();
            
            log.info("딜러 {} 메뉴 파싱 완료: 권한 {}개, 총 메뉴 {}개", 
                    dealerId, dealerMenus.size(), totalMenuCount);
            
        } catch (Exception e) {
            log.error("딜러 {} 메뉴 파싱 실패: {}", dealerId, e.getMessage(), e);
        }
    }
    
    /**
     * 특정 권한의 메뉴 블록 파싱
     */
    private void parseMenuByAuth(String jspContent, String authConstant, String authCode, 
                               Map<String, List<MenuInfo>> dealerMenus) {
        
        // 권한별 조건문 찾기 - 더 정확한 패턴 사용
        String pattern = String.format(
            "if\\s*\\(.*%s\\.equals\\(S_authSeq\\).*?\\)\\s*\\{([\\s\\S]*?)%%>\\s*<%.*?(?:else|%%>)",
            authConstant.replace(".", "\\.")
        );
        
        Pattern regex = Pattern.compile(pattern, Pattern.DOTALL);
        Matcher matcher = regex.matcher(jspContent);
        
        if (matcher.find()) {
            String menuBlock = matcher.group(1);
            List<MenuInfo> menus = parseMenuBlock(menuBlock);
            dealerMenus.put(authCode, menus);
            
            log.debug("{} 권한 메뉴 {}개 파싱됨", authCode, menus.size());
        } else {
            // 복수 권한 조건 체크 (예: SC || SL)
            String multiAuthPattern = String.format(
                "if\\s*\\([^{]*%s[^{]*\\)\\s*\\{([\\s\\S]*?)%%>\\s*<%.*?(?:else|%%>)",
                authConstant.replace(".", "\\.")
            );
            
            Pattern multiRegex = Pattern.compile(multiAuthPattern, Pattern.DOTALL);
            Matcher multiMatcher = multiRegex.matcher(jspContent);
            
            if (multiMatcher.find()) {
                String menuBlock = multiMatcher.group(1);
                List<MenuInfo> menus = parseMenuBlock(menuBlock);
                dealerMenus.put(authCode, menus);
                log.debug("{} 권한 메뉴 {}개 파싱됨 (복수 조건)", authCode, menus.size());
            } else {
                log.debug("{} 권한의 메뉴 블록을 찾을 수 없습니다", authCode);
            }
        }
    }
    
    /**
     * 메뉴 블록에서 개별 메뉴 정보 추출
     */
    private List<MenuInfo> parseMenuBlock(String menuBlock) {
        List<MenuInfo> menus = new ArrayList<>();
        
        // 메인 메뉴 파싱 패턴 개선
        Pattern mainMenuPattern = Pattern.compile(
            "<li><a[^>]*class=\"[^\"]*js-sub-menu-toggle[^\"]*\"[^>]*>.*?<span class=\"text\">([^<]+)</span>.*?<ul class=\"sub-menu\">(.*?)</ul>\\s*</li>", 
            Pattern.DOTALL
        );
        
        Matcher mainMatcher = mainMenuPattern.matcher(menuBlock);
        
        while (mainMatcher.find()) {
            String mainMenuName = mainMatcher.group(1).trim();
            String subMenuBlock = mainMatcher.group(2);
            
            // 아이콘 추출
            String iconClass = extractIconClass(mainMatcher.group(0));
            
            MenuInfo mainMenu = MenuInfo.builder()
                .name(mainMenuName)
                .level(1)
                .icon(iconClass)
                .children(new ArrayList<>())
                .build();
            
            // 서브 메뉴 파싱
            parseSubMenus(subMenuBlock, mainMenu);
            
            if (!mainMenu.getChildren().isEmpty()) {
                menus.add(mainMenu);
            }
        }
        
        return menus;
    }
    
    /**
     * 서브 메뉴 파싱 개선
     */
    private void parseSubMenus(String subMenuBlock, MenuInfo mainMenu) {
        // 다양한 서브 메뉴 패턴 처리
        Pattern[] subMenuPatterns = {
            // 기본 패턴
            Pattern.compile("<li><a href=\"\\$\\{ctx\\}([^\"]+)\"[^>]*><span class=\"text\">([^<]+)</span></a></li>"),
            // 조건부 메뉴 패턴
            Pattern.compile("<li[^>]*><a href=\"\\$\\{ctx\\}([^\"]+)\"[^>]*>([^<]+)</a></li>"),
            // noblock 속성 있는 패턴
            Pattern.compile("<li><a href=\"[^\"]*\" onclick=\"[^\"]*\" noblock><span[^>]*>([^<]+)</span></a></li>")
        };
        
        for (Pattern pattern : subMenuPatterns) {
            Matcher matcher = pattern.matcher(subMenuBlock);
            
            while (matcher.find()) {
                String url = null;
                String subMenuName = null;
                
                if (matcher.groupCount() >= 2) {
                    url = matcher.group(1);
                    subMenuName = matcher.group(2).trim();
                } else {
                    subMenuName = matcher.group(1).trim();
                }
                
                // HTML 태그 제거
                subMenuName = subMenuName.replaceAll("<[^>]+>", "").trim();
                
                if (StringUtils.isNotBlank(subMenuName)) {
                    MenuInfo subMenu = MenuInfo.builder()
                        .name(subMenuName)
                        .url(cleanUrl(url))
                        .level(2)
                        .parent(mainMenu.getName())
                        .build();
                    
                    mainMenu.getChildren().add(subMenu);
                }
            }
        }
    }
    
    /**
     * 아이콘 클래스 추출
     */
    private String extractIconClass(String menuHtml) {
        Pattern iconPattern = Pattern.compile("<i class=\"([^\"]+)\"");
        Matcher iconMatcher = iconPattern.matcher(menuHtml);
        
        if (iconMatcher.find()) {
            return iconMatcher.group(1);
        }
        return "fa fa-circle";
    }
    
    /**
     * URL 정리
     */
    private String cleanUrl(String url) {
        if (url == null) return "";
        
        url = url.replace("${ctx}", "").trim();
        
        if (!url.startsWith("/") && !url.isEmpty()) {
            url = "/" + url;
        }
        
        return url;
    }
    
    /**
     * 딜러별 권한별 메뉴 조회
     */
    public List<MenuInfo> getMenusByDealerAndAuth(String dealerId, String authSeq) {
        Map<String, List<MenuInfo>> dealerMenus = dealerMenuCache.get(dealerId);
        
        if (dealerMenus == null) {
            log.warn("딜러 {} 메뉴 정보가 없습니다. 다시 파싱을 시도합니다.", dealerId);
            parseDealerMenus(dealerId);
            dealerMenus = dealerMenuCache.get(dealerId);
        }
        
        if (dealerMenus == null) {
            return Collections.emptyList();
        }
        
        return dealerMenus.getOrDefault(authSeq, Collections.emptyList());
    }
    
    /**
     * 딜러별 메뉴 검색
     */
    public Optional<MenuInfo> findMenuByName(String menuName, String authSeq, String dealerId) {
        List<MenuInfo> menus = getMenusByDealerAndAuth(dealerId, authSeq);
        
        return menus.stream()
            .flatMap(main -> Stream.concat(
                Stream.of(main),
                main.getChildren().stream()
            ))
            .filter(menu -> isMenuNameMatch(menu.getName(), menuName))
            .findFirst();
    }
    
    /**
     * 메뉴명 매칭 로직
     */
    private boolean isMenuNameMatch(String menuName, String searchName) {
        if (menuName == null || searchName == null) return false;
        
        String normalizedMenuName = menuName.toLowerCase().replaceAll("\\s+", "");
        String normalizedSearchName = searchName.toLowerCase().replaceAll("\\s+", "");
        
        return normalizedMenuName.contains(normalizedSearchName) || 
               normalizedSearchName.contains(normalizedMenuName);
    }
    
    /**
     * 특정 딜러 메뉴 갱신
     */
    public void refreshDealerMenus(String dealerId) {
        log.info("딜러 {} 메뉴 갱신 시작", dealerId);
        parseDealerMenus(dealerId);
    }
    
    /**
     * 모든 딜러 메뉴 갱신
     */
    public void refreshAllMenus() {
        log.info("모든 딜러 메뉴 갱신 시작");
        parseAllDealerMenus();
    }
    
    /**
     * 딜러별 파싱 상태 조회
     */
    public Map<String, Object> getDealerParsingStatus() {
        Map<String, Object> status = new HashMap<>();
        
        for (String dealerId : dealerConfig.getActiveDealers()) {
            Map<String, List<MenuInfo>> dealerMenus = dealerMenuCache.get(dealerId);
            
            Map<String, Object> dealerStatus = new HashMap<>();
            dealerStatus.put("parsed", dealerMenus != null);
            dealerStatus.put("lastParsed", lastParsedTimes.get(dealerId));
            dealerStatus.put("authoritiesCount", dealerMenus != null ? dealerMenus.size() : 0);
            dealerStatus.put("totalMenuCount", dealerMenus != null ? 
                dealerMenus.values().stream()
                    .mapToInt(menus -> menus.stream()
                        .mapToInt(menu -> 1 + menu.getChildren().size())
                        .sum())
                    .sum() : 0);
            
            status.put(dealerId, dealerStatus);
        }
        
        return status;
    }
}