package com.example.controller;

import com.example.model.Job;
import com.example.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.List;

/** Generates sitemap.xml dynamically so every published job posting stays discoverable. */
@RestController
public class SitemapController {

    @Autowired
    private JobRepository jobRepository;

    @Value("${app.base-url:https://koderhyre.com}")
    private String baseUrl;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        appendUrl(xml, baseUrl + "/", null, "daily", "1.0");
        appendUrl(xml, baseUrl + "/jobs", null, "hourly", "0.9");
        appendUrl(xml, baseUrl + "/jobseeker/register", null, "monthly", "0.5");
        appendUrl(xml, baseUrl + "/jobseeker/login", null, "monthly", "0.3");
        appendUrl(xml, baseUrl + "/recruiter/home", null, "monthly", "0.5");
        appendUrl(xml, baseUrl + "/recruiter/register", null, "monthly", "0.5");
        appendUrl(xml, baseUrl + "/recruiter/login", null, "monthly", "0.3");

        List<Job> jobs = jobRepository.findAllPublishedForSitemap();
        for (Job job : jobs) {
            String lastmod = job.getPostedDate() != null
                    ? job.getPostedDate().toLocalDate().format(DATE_FMT)
                    : null;
            appendUrl(xml, baseUrl + "/jobs/" + job.getId(), lastmod, "weekly", "0.8");
        }

        xml.append("</urlset>");
        return ResponseEntity.ok(xml.toString());
    }

    private void appendUrl(StringBuilder xml, String loc, String lastmod, String changefreq, String priority) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(loc).append("</loc>\n");
        if (lastmod != null) {
            xml.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
        }
        xml.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        xml.append("    <priority>").append(priority).append("</priority>\n");
        xml.append("  </url>\n");
    }
}
