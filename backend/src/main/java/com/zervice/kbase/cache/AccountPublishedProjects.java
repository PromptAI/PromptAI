package com.zervice.kbase.cache;

import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.database.dao.PublishedProjectDao;
import com.zervice.kbase.database.pojo.PublishedProject;
import lombok.extern.log4j.Log4j2;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manage per-customer published project, saved in AccountCatalog ...
 */
@Log4j2
public class AccountPublishedProjects extends AccountCache {
    public final static String NAME = "PublishedProjects";
    public static final int AUTH_MODE = 2;

    public AccountPublishedProjects(AccountCatalog catalog) {
        super(catalog, NAME);
    }

    private final ConcurrentHashMap<String/* published project id*/, PublishedProject> _projects = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String/* agent id*/, Set<PublishedProject>> _agentProjects = new ConcurrentHashMap<>();


    public void add(PublishedProject publishedProject) {
        _projects.put(publishedProject.getId(), publishedProject);
        _agentProjects.putIfAbsent(publishedProject.getAgentId(), new HashSet<>());
        // 有可能 PublishedProject 已存在 （id+agentId） 新的就放不进去
        // 所以这里先remove 再add
        Set<PublishedProject> agentProjects = _agentProjects.get(publishedProject.getAgentId());
        agentProjects.remove(publishedProject);
        agentProjects.add(publishedProject);
    }

    public boolean exits(String publishedProjectId) {
        return _projects.containsKey(publishedProjectId);
    }

    public Set<PublishedProject> getByAgentId(String agentId) {
        return _agentProjects.getOrDefault(agentId, new HashSet<>());
    }

    public PublishedProject get(String publishedProjectId) {
        return _projects.get(publishedProjectId);
    }


    public void remove(String publishedProjectId) {
        PublishedProject removed = _projects.remove(publishedProjectId);
        if (removed != null) {
            _agentProjects.getOrDefault(removed.getAgentId(), new HashSet<>()).remove(removed);
        }
    }

    public void onUpdate(PublishedProject publishedProject) {
        _projects.put(publishedProject.getId(), publishedProject);
        _agentProjects.putIfAbsent(publishedProject.getAgentId(), new HashSet<>());

        Set<PublishedProject> agentProjectsSet = _agentProjects.get(publishedProject.getAgentId());
        agentProjectsSet.remove(publishedProject);
        agentProjectsSet.add(publishedProject);
    }

    public int size() {
        return _projects.size();
    }

    @Override
    protected void _initialize(Connection conn) {
        try {
            List<PublishedProject> publishedProjects = PublishedProjectDao.getAll(conn, _account.getDBName()).stream()
                    .filter(p -> PublishedProject.VALID_STATUS.contains(p.getStatus()))
                    .collect(Collectors.toList());
            for (PublishedProject publishedProject : publishedProjects) {
                add(publishedProject);
            }
        } catch (SQLException e) {
            LOG.error("cannot init access token cache e:{}", e.getMessage(), e);
        }
    }

    @Override
    protected void _reload(Connection conn) {
        //nothing to do
    }
}
