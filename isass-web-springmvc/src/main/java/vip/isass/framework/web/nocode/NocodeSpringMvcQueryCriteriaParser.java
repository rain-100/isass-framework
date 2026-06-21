package vip.isass.framework.web.nocode;

import org.springframework.util.MultiValueMap;
import vip.isass.framework.nocode.v3.query.NocodeBlankStringPolicy;
import vip.isass.framework.nocode.v3.query.NocodeQueryCriteria;
import vip.isass.framework.nocode.v3.query.NocodeSort;

import java.util.List;
import java.util.Set;

/**
 * Parses Spring MVC query parameters into nocode v3 query criteria.
 */
public class NocodeSpringMvcQueryCriteriaParser {

    public static final String PARAM_PAGE = "page";
    public static final String PARAM_PAGE_NUMBER = "pageNumber";
    public static final String PARAM_PAGE_SIZE = "pageSize";
    public static final String PARAM_SIZE = "size";
    public static final String PARAM_SORT = "sort";
    public static final String PARAM_SELECT = "select";

    private static final int DEFAULT_PAGE_NUMBER = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final Set<String> RESERVED_PARAMETERS = Set.of(
            PARAM_PAGE,
            PARAM_PAGE_NUMBER,
            PARAM_PAGE_SIZE,
            PARAM_SIZE,
            PARAM_SORT,
            PARAM_SELECT
    );

    private final NocodeBlankStringPolicy blankStringPolicy;

    public NocodeSpringMvcQueryCriteriaParser() {
        this(NocodeBlankStringPolicy.MATCH_BLANK);
    }

    public NocodeSpringMvcQueryCriteriaParser(NocodeBlankStringPolicy blankStringPolicy) {
        this.blankStringPolicy = blankStringPolicy == null ? NocodeBlankStringPolicy.MATCH_BLANK : blankStringPolicy;
    }

    public NocodeQueryCriteria parse(MultiValueMap<String, String> parameters) {
        NocodeQueryCriteria.Builder builder = NocodeQueryCriteria.builder()
                .blankStringPolicy(blankStringPolicy);
        if (parameters == null || parameters.isEmpty()) {
            return builder.build();
        }

        parameters.forEach((name, values) -> {
            if (!RESERVED_PARAMETERS.contains(name)) {
                builder.where(name, value(values));
            }
        });
        applyPage(builder, parameters);
        applySort(builder, parameters.get(PARAM_SORT));
        applySelect(builder, parameters.get(PARAM_SELECT));
        return builder.build();
    }

    private void applyPage(NocodeQueryCriteria.Builder builder, MultiValueMap<String, String> parameters) {
        String pageValue = first(parameters, PARAM_PAGE_NUMBER, PARAM_PAGE);
        String pageSizeValue = first(parameters, PARAM_PAGE_SIZE, PARAM_SIZE);
        if (pageValue == null && pageSizeValue == null) {
            return;
        }
        int pageNumber = pageValue == null ? DEFAULT_PAGE_NUMBER : parsePositiveInt(pageValue, PARAM_PAGE_NUMBER);
        int pageSize = pageSizeValue == null ? DEFAULT_PAGE_SIZE : parsePositiveInt(pageSizeValue, PARAM_PAGE_SIZE);
        builder.page(pageNumber, pageSize);
    }

    private void applySort(NocodeQueryCriteria.Builder builder, List<String> values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            String fieldName = value == null ? "" : value.trim();
            NocodeSort.Direction direction = NocodeSort.Direction.ASC;
            int commaIndex = fieldName.indexOf(',');
            if (commaIndex >= 0) {
                direction = parseDirection(fieldName.substring(commaIndex + 1));
                fieldName = fieldName.substring(0, commaIndex).trim();
            } else if (fieldName.startsWith("-")) {
                direction = NocodeSort.Direction.DESC;
                fieldName = fieldName.substring(1).trim();
            }
            if (!fieldName.isBlank()) {
                builder.sort(fieldName, direction);
            }
        }
    }

    private void applySelect(NocodeQueryCriteria.Builder builder, List<String> values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            split(value).stream()
                    .map(String::trim)
                    .filter(field -> !field.isBlank())
                    .forEach(builder::select);
        }
    }

    private Object value(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        if (values.size() == 1) {
            return values.getFirst();
        }
        return List.copyOf(values);
    }

    private String first(MultiValueMap<String, String> parameters, String preferredName, String fallbackName) {
        String value = parameters.getFirst(preferredName);
        return value == null ? parameters.getFirst(fallbackName) : value;
    }

    private int parsePositiveInt(String value, String parameterName) {
        try {
            int number = Integer.parseInt(value);
            if (number > 0) {
                return number;
            }
        } catch (NumberFormatException ignored) {
            // handled below with a stable message
        }
        throw new IllegalArgumentException(parameterName + " must be a positive integer");
    }

    private NocodeSort.Direction parseDirection(String value) {
        if ("desc".equalsIgnoreCase(value.trim())) {
            return NocodeSort.Direction.DESC;
        }
        return NocodeSort.Direction.ASC;
    }

    private List<String> split(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.of(value.split(","));
    }
}
