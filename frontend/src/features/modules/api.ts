import { apiFetch } from "../../api/client";
import type { ModuleDetails } from "../../api/types";

export async function getModuleDetails(moduleKey: string) {
    return apiFetch<ModuleDetails>(`/modules/${moduleKey}`);
}
