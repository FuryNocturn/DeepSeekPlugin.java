package es.furynocturntv.mcreator.deepseek.services;

import net.mcreator.workspace.Workspace;

public class MCreatorWorkspace {
    private final Workspace workspace;

    public MCreatorWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public Workspace getWorkspace() {
        return workspace;
    }
}