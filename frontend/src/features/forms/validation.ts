import type { FieldDef } from "../../api/types";

export type ValidationErrors = Record<string, string>;

function isEmpty(value: unknown): boolean {
    if (value === null || value === undefined) return true;
    if (typeof value === "string") return value.trim().length === 0;
    if (Array.isArray(value)) return value.length === 0;
    return false;
}

function parseNumber(value: unknown): number | null {
    if (value === null || value === undefined || value === "") return null;
    if (typeof value === "number") return Number.isNaN(value) ? null : value;
    const parsed = Number(value);
    return Number.isNaN(parsed) ? null : parsed;
}

function isValidDate(value: unknown): boolean {
    if (value === null || value === undefined || value === "") return true;
    const date = new Date(String(value));
    return !Number.isNaN(date.getTime());
}

export function validateField(field: FieldDef, value: unknown): string | undefined {
    const label = field.label || field.key;
    const flags = field.flags || {};
    const constraints = field.constraints || {};

    if (flags.required && isEmpty(value)) {
        return `${label} is required`;
    }

    if (isEmpty(value)) return undefined;

    switch (field.type) {
        case "TEXT": {
            const str = String(value);
            if (constraints.minLength !== undefined && str.length < constraints.minLength) {
                return `${label} must be at least ${constraints.minLength} characters`;
            }
            if (constraints.maxLength !== undefined && str.length > constraints.maxLength) {
                return `${label} must be at most ${constraints.maxLength} characters`;
            }
            if (constraints.pattern) {
                const regex = new RegExp(constraints.pattern);
                if (!regex.test(str)) {
                    return `${label} does not match required pattern`;
                }
            }
            break;
        }
        case "NUMBER": {
            const num = parseNumber(value);
            if (num === null) return `${label} must be a number`;
            if (constraints.min !== undefined && num < constraints.min) {
                return `${label} must be ≥ ${constraints.min}`;
            }
            if (constraints.max !== undefined && num > constraints.max) {
                return `${label} must be ≤ ${constraints.max}`;
            }
            break;
        }
        case "DATE": {
            if (!isValidDate(value)) {
                return `${label} must be a valid date`;
            }
            if (constraints.min && new Date(String(value)).getTime() < new Date(constraints.min).getTime()) {
                return `${label} must be on or after ${constraints.min}`;
            }
            if (constraints.max && new Date(String(value)).getTime() > new Date(constraints.max).getTime()) {
                return `${label} must be on or before ${constraints.max}`;
            }
            break;
        }
        case "ENUM": {
            if (constraints.multi && Array.isArray(value)) {
                if (flags.required && value.length === 0) return `${label} is required`;
            }
            break;
        }
        case "TAGS": {
            if (flags.required && Array.isArray(value) && value.length === 0) {
                return `${label} is required`;
            }
            break;
        }
        case "JSON": {
            if (typeof value === "string") {
                try {
                    JSON.parse(value);
                } catch {
                    return `${label} must be valid JSON`;
                }
            }
            break;
        }
        default:
            break;
    }

    return undefined;
}

export function validateAttributes(fields: FieldDef[], attributes: Record<string, any>): ValidationErrors {
    const errors: ValidationErrors = {};
    fields.forEach((field) => {
        const error = validateField(field, attributes[field.key]);
        if (error) errors[field.key] = error;
    });
    return errors;
}
