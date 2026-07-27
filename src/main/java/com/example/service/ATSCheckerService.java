package com.example.service;

import com.example.model.Job;
import com.example.model.JobseekerProfile;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ATSCheckerService {

    public ATSMatchResult checkMatch(JobseekerProfile profile, String jobDescription, String jobTitle) {
        if (profile == null || jobDescription == null || jobDescription.isBlank()) {
            return new ATSMatchResult(0, "Invalid input", new HashMap<>());
        }

        Set<String> candidateSkillSet = candidateSkills(profile);
        Set<String> jobSkillKeywords = extractKeywords(jobDescription, candidateSkillSet);
        List<String> matchedSkills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();
        for (String skill : jobSkillKeywords) {
            boolean has = candidateSkillSet.stream().anyMatch(c -> skillMatches(c, skill));
            if (has) matchedSkills.add(skill);
            else missingSkills.add(skill);
        }

        int skillsScore;
        if (jobSkillKeywords.isEmpty()) {
            skillsScore = 100; // JD lists no recognizable tech skills — don't penalize
        } else if (candidateSkillSet.isEmpty()) {
            skillsScore = 0;
        } else {
            skillsScore = (matchedSkills.size() * 100) / jobSkillKeywords.size();
        }

        int experienceScore = matchExperience(profile, jobDescription);
        int educationScore = matchEducation(profile, jobDescription);
        int workModeScore = (profile.getWorkMode() != null ? 80 : 50);
        int overallScore = (int) ((skillsScore * 0.40) + (experienceScore * 0.35) + (educationScore * 0.15) + (workModeScore * 0.10));

        // Skills are the core of an ATS match — a candidate missing most required skills
        // must not be labeled a good match just because experience/education line up.
        if (!jobSkillKeywords.isEmpty()) {
            if (skillsScore < 35)      overallScore = Math.min(overallScore, 45);
            else if (skillsScore < 60) overallScore = Math.min(overallScore, 74);
        }

        Map<String, Object> breakdown = new LinkedHashMap<>();
        breakdown.put("skillsScore", skillsScore);
        breakdown.put("experienceScore", experienceScore);
        breakdown.put("educationScore", educationScore);
        breakdown.put("workModeScore", workModeScore);
        breakdown.put("overallScore", overallScore);
        breakdown.put("matchedSkills", matchedSkills);
        breakdown.put("missingSkills", missingSkills);

        String feedback = generateFeedback(skillsScore, experienceScore, educationScore, overallScore, missingSkills);
        return new ATSMatchResult(overallScore, feedback, breakdown);
    }

    private Set<String> candidateSkills(JobseekerProfile profile) {
        String csv = ((profile.getPrimarySkills() != null ? profile.getPrimarySkills() : "") + "," +
                (profile.getSkills() != null ? profile.getSkills() : "")).toLowerCase();
        return parseSkills(csv);
    }

    private int matchExperience(JobseekerProfile profile, String jobDescription) {
        if (profile.getExperienceYears() == null) return 50;

        Integer candidateExp = profile.getExperienceYears();
        Integer requiredExp = extractExperienceRequirement(jobDescription);

        if (requiredExp == null || requiredExp == 0) return 100;

        if (candidateExp < requiredExp) {
            return Math.max(0, 100 - ((requiredExp - candidateExp) * 15));
        }
        if (candidateExp > requiredExp + 3) {
            return 85; // slightly overqualified
        }
        return 100;
    }

    private int matchEducation(JobseekerProfile profile, String jobDescription) {
        if (profile.getHighestEducation() == null) return 50;

        String education = profile.getHighestEducation().toLowerCase();
        String jobDesc = jobDescription.toLowerCase();

        if (jobDesc.contains("btech") || jobDesc.contains("b.tech") || jobDesc.contains("bachelor")) {
            return (education.contains("btech") || education.contains("b.tech") || education.contains("bachelor")) ? 100 : 70;
        }
        if (jobDesc.contains("mtech") || jobDesc.contains("m.tech") || jobDesc.contains("master")) {
            return (education.contains("mtech") || education.contains("m.tech") || education.contains("master")) ? 100 : 60;
        }
        return 80; // generic match
    }

    // Concrete tech-stack terms only. Generic process words (rest, api, agile, git,
    // microservices, linux…) are excluded — nearly every JD contains them, and counting
    // them unfairly drags down candidates who don't list them as "skills".
    private static final String[] COMMON_SKILLS = {
        // languages
        "java", "python", "javascript", "typescript", "kotlin", "golang", "rust", "c++", "c#", "php", "ruby",
        "scala", "perl", "swift", "dart", "elixir", "matlab",
        // backend / frameworks
        "spring", "django", "flask", "fastapi", "express", "nestjs", "laravel", "node",
        // frontend
        "react", "angular", "vue", "svelte", "next.js", "jquery", "bootstrap", "tailwind", "html", "css", "sass",
        // mobile
        "flutter", "android", "ios", "react native",
        // data stores
        "sql", "nosql", "mongodb", "postgresql", "mysql", "oracle", "sqlite", "redis",
        "elasticsearch", "dynamodb", "cassandra", "neo4j", "snowflake", "databricks",
        // cloud / infra
        "aws", "azure", "gcp", "docker", "kubernetes", "terraform", "ansible", "jenkins",
        "nginx", "firebase", "heroku", "devops", "prometheus", "grafana",
        // data / ML
        "kafka", "rabbitmq", "hadoop", "spark", "airflow", "tensorflow", "pytorch",
        "scikit-learn", "pandas", "numpy", "machine learning", "deep learning",
        // testing & tools
        "selenium", "cypress", "junit", "jmeter", "appium", "postman", "graphql",
        // enterprise / analytics / design
        "salesforce", "sap", "tableau", "power bi", "figma", "wordpress", "shopify"
    };

    /**
     * Skills the JD asks for: dictionary terms found in the text (whole-word match),
     * plus any of the candidate's own listed skills that appear in the JD — so niche
     * skills outside the dictionary (e.g. a specific tool) still count when both sides have them.
     */
    private Set<String> extractKeywords(String text, Set<String> candidateSkillSet) {
        Set<String> keywords = new TreeSet<>();
        for (String skill : COMMON_SKILLS) {
            if (containsTerm(text, skill)) keywords.add(skill);
        }
        for (String cs : candidateSkillSet) {
            if (cs.length() > 2 && containsTerm(text, cs)) keywords.add(cs);
        }
        return keywords;
    }

    /** Whole-word occurrence check: "java" does NOT match inside "javascript". */
    private static boolean containsTerm(String text, String term) {
        if (term.isBlank()) return false;
        String prefix = Character.isLetterOrDigit(term.charAt(0)) ? "\\b" : "";
        String suffix = Character.isLetterOrDigit(term.charAt(term.length() - 1)) ? "\\b" : "";
        return Pattern.compile(prefix + Pattern.quote(term) + suffix, Pattern.CASE_INSENSITIVE)
                .matcher(text).find();
    }

    /** True if the candidate skill satisfies the JD skill (exact, or whole-word containment either way). */
    private static boolean skillMatches(String candidateSkill, String jdSkill) {
        if (candidateSkill.equalsIgnoreCase(jdSkill)) return true;
        return containsTerm(candidateSkill, jdSkill) || containsTerm(jdSkill, candidateSkill);
    }

    private Set<String> parseSkills(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank() && s.length() > 1)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    private Integer extractExperienceRequirement(String jobDescription) {
        String lower = jobDescription.toLowerCase();

        // Look for patterns like "2 years", "3+ years", "2-5 years"
        if (lower.contains("5+ years") || lower.contains("5 years")) return 5;
        if (lower.contains("4+ years") || lower.contains("4 years")) return 4;
        if (lower.contains("3+ years") || lower.contains("3 years")) return 3;
        if (lower.contains("2+ years") || lower.contains("2 years")) return 2;
        if (lower.contains("1+ year") || lower.contains("1 year")) return 1;
        if (lower.contains("fresher") || lower.contains("entry level") || lower.contains("0 years")) return 0;

        return null; // not specified
    }

    private String generateFeedback(int skillsScore, int experienceScore, int educationScore, int overall,
                                    List<String> missingSkills) {
        StringBuilder feedback = new StringBuilder();

        String missing = missingSkills.isEmpty() ? "" : String.join(", ", missingSkills);
        if (skillsScore >= 80) {
            feedback.append("✓ Strong skill match. ");
            if (!missing.isEmpty()) feedback.append("To stand out further, add: ").append(missing).append(". ");
        } else if (skillsScore >= 60) {
            feedback.append("○ Moderate skill match. Add these skills to your resume if you have them: ")
                    .append(missing).append(". ");
        } else {
            feedback.append("✗ Limited skill match. The job asks for: ").append(missing)
                    .append(" — these are missing from your profile. ");
        }

        if (experienceScore >= 90) {
            feedback.append("✓ Experience aligns well. ");
        } else if (experienceScore >= 55) {
            feedback.append("○ You're close to the stated experience requirement — highlight relevant projects to bridge the gap. ");
        } else {
            feedback.append("✗ You're below the experience requirement. ");
        }

        if (educationScore >= 80) {
            feedback.append("✓ Education requirement met. ");
        } else if (educationScore >= 60) {
            feedback.append("○ Education partially matches. ");
        } else {
            feedback.append("○ Consider adding your education details. ");
        }

        if (overall >= 80) {
            feedback.append("\n🎯 Excellent match! Apply now.");
        } else if (overall >= 60) {
            feedback.append("\n⚠ Good match, but some gaps exist. Consider improving your profile.");
        } else if (overall >= 40) {
            feedback.append("\n📚 Moderate match. Upskill in the missing areas.");
        } else {
            feedback.append("\n❌ Poor match. This role may not be the right fit yet.");
        }

        return feedback.toString();
    }

    public static class ATSMatchResult {
        public int score;
        public String feedback;
        public Map<String, Object> breakdown;

        public ATSMatchResult(int score, String feedback, Map<String, Object> breakdown) {
            this.score = score;
            this.feedback = feedback;
            this.breakdown = breakdown;
        }

        public int getScore() { return score; }
        public String getFeedback() { return feedback; }
        public Map<String, Object> getBreakdown() { return breakdown; }
    }
}
