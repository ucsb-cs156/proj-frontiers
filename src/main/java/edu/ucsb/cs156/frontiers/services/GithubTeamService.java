package edu.ucsb.cs156.frontiers.services;

import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.repositories.TeamRepository;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GithubTeamService {

    private final TeamRepository teamRepository;
    private final RestTemplate template;

    public GithubTeamService(TeamRepository teamRepository, RestTemplateBuilder builder) {
        this.teamRepository = teamRepository;
        this.template = builder.build();
    }

    public static String slugify(String name) {
        return name.toLowerCase().replaceAll(" ", "-");
    }

    public void upsertTeam(Team team){

    }
}
