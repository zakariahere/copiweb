package com.elzakaria.copiweb.dto;

import java.util.List;

public record AgentCatalogView(
    List<DiscoveredAgentView> agents,
    List<InstalledPluginView> installedPlugins,
    List<RegisteredMarketplaceView> marketplaces
) {
}
