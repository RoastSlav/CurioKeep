import { FormControl, InputLabel, MenuItem, Select, Stack, Tab, Tabs, Typography, useMediaQuery } from "@mui/material";
import { useTheme } from "@mui/material/styles";
import type { CollectionModule } from "../../../api/types";

type Props = {
    modules: CollectionModule[];
    activeModuleKey: string | null;
    onChange: (moduleKey: string) => void;
};

export default function ModuleSelector({ modules, activeModuleKey, onChange }: Props) {
    const theme = useTheme();
    const isMobile = useMediaQuery(theme.breakpoints.down("sm"));

    if (!modules.length) {
        return (
            <Typography color="text.secondary" variant="body2">
                No modules are enabled for this collection yet.
            </Typography>
        );
    }

    if (isMobile) {
        return (
            <FormControl fullWidth size="small">
                <InputLabel id="collection-module-selector">Module</InputLabel>
                <Select
                    labelId="collection-module-selector"
                    label="Module"
                    value={activeModuleKey || modules[0].moduleKey}
                    onChange={(e) => onChange(e.target.value)}
                >
                    {modules.map((module) => (
                        <MenuItem key={module.moduleKey} value={module.moduleKey}>
                            <Stack direction="column" spacing={0.25}>
                                <Typography fontWeight={600}>{module.name || module.moduleKey}</Typography>
                                {module.version && (
                                    <Typography variant="caption" color="text.secondary">
                                        v{module.version}
                                    </Typography>
                                )}
                            </Stack>
                        </MenuItem>
                    ))}
                </Select>
            </FormControl>
        );
    }

    return (
        <Tabs
            value={activeModuleKey || modules[0].moduleKey}
            onChange={(_, value) => onChange(value)}
            variant="scrollable"
            scrollButtons="auto"
        >
            {modules.map((module) => (
                <Tab
                    key={module.moduleKey}
                    value={module.moduleKey}
                    label={
                        <Stack direction="row" spacing={1} alignItems="center">
                            <Typography fontWeight={600}>{module.name || module.moduleKey}</Typography>
                            {module.version && (
                                <Typography variant="caption" color="text.secondary">
                                    v{module.version}
                                </Typography>
                            )}
                        </Stack>
                    }
                />
            ))}
        </Tabs>
    );
}
