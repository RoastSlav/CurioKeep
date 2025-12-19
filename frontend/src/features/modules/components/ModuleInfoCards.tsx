import { Box, Card, CardContent, Chip, Link, Stack, Typography } from "@mui/material";
import type { ModuleDetails } from "../api/modulesApi";

type Props = {
    module: ModuleDetails;
};

export default function ModuleInfoCards({ module }: Props) {
    const meta = module.contract.meta;
    const tags = meta?.tags ?? [];
    const authors = meta?.authors ?? [];

    const summaryItems = [
        { label: "Version", value: module.version },
        { label: "Source", value: module.source },
        { label: "Checksum", value: module.checksum },
        { label: "Updated", value: new Date(module.updatedAt).toLocaleString() },
    ];

    const metaItems = [
        { label: "Repository", value: meta?.repository ?? "—", href: meta?.repository },
        { label: "Homepage", value: meta?.homepage ?? "—", href: meta?.homepage },
        { label: "License", value: meta?.license ?? "—" },
        { label: "Min App", value: meta?.minAppVersion ?? "—" },
    ];

    return (
        <Box sx={{ display: "grid", gap: 2, gridTemplateColumns: { xs: "1fr", md: "repeat(2, 1fr)" } }}>
            <Box>
                <Card>
                    <CardContent>
                        <Stack spacing={0.5}>
                            <Typography variant="subtitle2" color="text.secondary">
                                Module summary
                            </Typography>
                            {summaryItems.map((item) => (
                                <Stack key={item.label} direction="row" justifyContent="space-between">
                                    <Typography variant="body2" color="text.secondary">
                                        {item.label}
                                    </Typography>
                                    <Typography variant="body2">{item.value}</Typography>
                                </Stack>
                            ))}
                        </Stack>
                    </CardContent>
                </Card>
            </Box>

            <Box>
                <Card>
                    <CardContent>
                        <Stack spacing={0.5}>
                            <Typography variant="subtitle2" color="text.secondary">
                                Module metadata
                            </Typography>
                            {metaItems.map((item) => (
                                <Stack key={item.label} direction="row" justifyContent="space-between">
                                    <Typography variant="body2" color="text.secondary">
                                        {item.label}
                                    </Typography>
                                    {item.href ? (
                                        <Link href={item.href} target="_blank" rel="noreferrer" underline="hover">
                                            {item.value}
                                        </Link>
                                    ) : (
                                        <Typography variant="body2">{item.value}</Typography>
                                    )}
                                </Stack>
                            ))}
                        </Stack>
                    </CardContent>
                </Card>
            </Box>

            {tags.length > 0 && (
                <Box>
                    <Card>
                        <CardContent>
                            <Stack spacing={0.5}>
                                <Typography variant="subtitle2" color="text.secondary">
                                    Tags
                                </Typography>
                                <Stack direction="row" spacing={0.75} flexWrap="wrap">
                                    {tags.map((tag) => (
                                        <Chip key={tag} label={tag} size="small" />
                                    ))}
                                </Stack>
                            </Stack>
                        </CardContent>
                    </Card>
                </Box>
            )}

            {authors.length > 0 && (
                <Box>
                    <Card>
                        <CardContent>
                            <Stack spacing={0.5}>
                                <Typography variant="subtitle2" color="text.secondary">
                                    Authors
                                </Typography>
                                {authors.map((author) => (
                                    <Stack key={`${author.name ?? author.email ?? author.url ?? ""}`} spacing={0.25}>
                                        {author.name && (
                                            <Typography variant="body2">
                                                {author.name}
                                            </Typography>
                                        )}
                                        {(author.email || author.url) && (
                                            <Typography variant="caption" color="text.secondary">
                                                {[author.email, author.url].filter(Boolean).join(" • ")}
                                            </Typography>
                                        )}
                                    </Stack>
                                ))}
                            </Stack>
                        </CardContent>
                    </Card>
                </Box>
            )}
        </Box>
    );
}
