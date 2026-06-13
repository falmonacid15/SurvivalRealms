package org.falmdev.survivalRealms.presentation.menu;

import java.util.List;
import java.util.Map;

public class MenuDefinition {

    private final String                   id;
    private final String                   name;
    private final int                      size;
    private final boolean                  useLayout;
    private final boolean                  paginated;
    private final String                   parent;
    private final Map<String, ItemDefinition> items;
    private final PaginatedItemDefinition  paginatedItem;

    public MenuDefinition(String id, String name, int size, boolean useLayout,
                          boolean paginated, String parent,
                          Map<String, ItemDefinition> items,
                          PaginatedItemDefinition paginatedItem) {
        this.id            = id;
        this.name          = name;
        this.size          = size;
        this.useLayout     = useLayout;
        this.paginated     = paginated;
        this.parent        = parent;
        this.items         = items;
        this.paginatedItem = paginatedItem;
    }

    public String                      getId()            { return id; }
    public String                      getName()          { return name; }
    public int                         getSize()          { return size; }
    public boolean                     isUseLayout()      { return useLayout; }
    public boolean                     isPaginated()      { return paginated; }
    public String                      getParent()        { return parent; }
    public Map<String, ItemDefinition> getItems()         { return items; }
    public PaginatedItemDefinition     getPaginatedItem() { return paginatedItem; }

    public static class ItemDefinition {
        public final int          slot;
        public final String       material;
        public final String       name;
        public final List<String> lore;
        public final String       action;
        public final String       actionData;

        public ItemDefinition(int slot, String material, String name,
                              List<String> lore, String action, String actionData) {
            this.slot       = slot;
            this.material   = material;
            this.name       = name;
            this.lore       = lore;
            this.action     = action;
            this.actionData = actionData;
        }
    }

    public static class PaginatedItemDefinition {
        public final String       material;
        public final String       name;
        public final List<String> lore;
        public final String       action;
        public final String       actionData;

        public PaginatedItemDefinition(String material, String name,
                                       List<String> lore, String action, String actionData) {
            this.material   = material;
            this.name       = name;
            this.lore       = lore;
            this.action     = action;
            this.actionData = actionData;
        }
    }
}