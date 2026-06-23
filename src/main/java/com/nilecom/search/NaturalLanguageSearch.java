package com.nilecom.search;

import com.nilecom.domain.Product;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tiny natural-language catalogue search. Parses a free-text query like
 * "cheap usb cable under $10" into price constraints, a sort hint, and keyword
 * terms, then filters and orders products. No external dependencies, no LLM —
 * deterministic and unit-testable.
 */
public final class NaturalLanguageSearch {

    /** Parsed intent of a query. */
    public static final class Query {
        public final List<String> terms;
        public final Double maxPrice;
        public final Double minPrice;
        public final Sort sort;

        Query(List<String> terms, Double maxPrice, Double minPrice, Sort sort) {
            this.terms = terms;
            this.maxPrice = maxPrice;
            this.minPrice = minPrice;
            this.sort = sort;
        }
    }

    public enum Sort { RELEVANCE, PRICE_ASC, PRICE_DESC }

    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
            "a", "an", "the", "for", "with", "and", "or", "me", "i", "want",
            "need", "find", "show", "some", "any", "of", "to", "in", "that",
            "is", "are", "please", "looking", "search", "than", "less", "more",
            "dollars", "dollar", "bucks", "buck", "price", "priced", "cost",
            "costs", "around", "about"));

    private static final Pattern UNDER =
            Pattern.compile("(?:under|below|less than|cheaper than|max|up to)\\s*\\$?\\s*(\\d+(?:\\.\\d+)?)");
    private static final Pattern OVER =
            Pattern.compile("(?:over|above|more than|at least|min)\\s*\\$?\\s*(\\d+(?:\\.\\d+)?)");
    private static final Pattern BARE_PRICE = Pattern.compile("\\$\\s*(\\d+(?:\\.\\d+)?)");

    private NaturalLanguageSearch() {}

    public static Query parse(String raw) {
        String q = raw == null ? "" : raw.toLowerCase().trim();

        Double max = null, min = null;
        Matcher mu = UNDER.matcher(q);
        if (mu.find()) max = Double.valueOf(mu.group(1));
        Matcher mo = OVER.matcher(q);
        if (mo.find()) min = Double.valueOf(mo.group(1));
        if (max == null && min == null) {
            Matcher mb = BARE_PRICE.matcher(q);
            if (mb.find()) max = Double.valueOf(mb.group(1)); // "$5" => up to $5
        }

        Sort sort = Sort.RELEVANCE;
        if (q.contains("cheap") || q.contains("cheapest") || q.contains("budget")
                || q.contains("affordable") || q.contains("low price")) {
            sort = Sort.PRICE_ASC;
        } else if (q.contains("expensive") || q.contains("premium")
                || q.contains("priciest") || q.contains("high end")
                || q.contains("high-end")) {
            sort = Sort.PRICE_DESC;
        }

        // Strip price phrases, then tokenize remaining words into search terms.
        String cleaned = q
                .replaceAll(UNDER.pattern(), " ")
                .replaceAll(OVER.pattern(), " ")
                .replaceAll(BARE_PRICE.pattern(), " ")
                .replaceAll("[^a-z0-9 ]", " ");

        List<String> terms = new ArrayList<>();
        for (String tok : cleaned.split("\\s+")) {
            if (tok.isEmpty()) continue;
            if (STOPWORDS.contains(tok)) continue;
            if (tok.equals("cheap") || tok.equals("cheapest") || tok.equals("budget")
                    || tok.equals("affordable") || tok.equals("expensive")
                    || tok.equals("premium") || tok.equals("priciest")) continue;
            if (tok.matches("\\d+")) continue; // leftover bare numbers
            terms.add(tok);
        }
        return new Query(terms, max, min, sort);
    }

    public static List<Product> apply(Query query, List<Product> catalogue) {
        List<Product> out = new ArrayList<>();
        for (Product p : catalogue) {
            if (query.maxPrice != null && p.price() > query.maxPrice) continue;
            if (query.minPrice != null && p.price() < query.minPrice) continue;
            if (!matchesTerms(query.terms, p.description())) continue;
            out.add(p);
        }
        if (query.sort == Sort.PRICE_ASC) {
            out.sort(Comparator.comparingDouble(Product::price));
        } else if (query.sort == Sort.PRICE_DESC) {
            out.sort(Comparator.comparingDouble(Product::price).reversed());
        }
        return out;
    }

    public static List<Product> search(String raw, List<Product> catalogue) {
        return apply(parse(raw), catalogue);
    }

    private static boolean matchesTerms(List<String> terms, String description) {
        if (terms.isEmpty()) return true;
        String d = description.toLowerCase();
        for (String t : terms) {
            if (!d.contains(t)) return false; // AND match
        }
        return true;
    }
}
