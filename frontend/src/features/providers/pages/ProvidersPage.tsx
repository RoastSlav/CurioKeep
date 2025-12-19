"use client";

import { useState, useEffect, useMemo } from "react";
import { Search, ExternalLink, Check, AlertCircle, Info } from "lucide-react";
import { Input } from "../../../../components/ui/input";
import { Button } from "../../../../components/ui/button";
import { Badge } from "../../../../components/ui/badge";
import ErrorState from "../../../components/ErrorState";
import { useToast } from "../../../components/Toasts";
import { listProviders, testProviderConnection } from "../api";
import { ProviderCredentialsModal } from "../components/ProviderCredentialsModal";
import { providerAttribution } from "../providerAttribution";
import type { Provider } from "../providerTypes";

export default function ProvidersPage() {
  const [providers, setProviders] = useState<Provider[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [sortBy, setSortBy] = useState<"configured" | "name">("configured");
  const [selectedProvider, setSelectedProvider] = useState<Provider | null>(
    null
  );
  const [testingProvider, setTestingProvider] = useState<string | null>(null);
  const [checkResults, setCheckResults] = useState<
    Record<string, "success" | "failed" | null>
  >({});
  const { showToast } = useToast();

  useEffect(() => {
    loadProviders();
  }, []);

  async function loadProviders() {
    try {
      setLoading(true);
      setError(null);
      const data = await listProviders();
      const normalizedData = Array.isArray(data)
        ? data.map((p) => ({
            ...p,
            supportsIdentifiers: Array.isArray((p as any).supportsIdentifiers)
              ? (p as any).supportsIdentifiers
              : [],
            supportedIdTypes: Array.isArray((p as any).supportedIdTypes)
              ? (p as any).supportedIdTypes
              : [],
            credentialFields: Array.isArray((p as any).credentialFields)
              ? (p as any).credentialFields
              : [],
            highlights: Array.isArray((p as any).highlights)
              ? (p as any).highlights
              : [],
          }))
        : [];
      setProviders(normalizedData);
    } catch (err: any) {
      console.error("[v0] Failed to load providers:", err);
      setError(err.message || "Failed to load providers");
      setProviders([]);
    } finally {
      setLoading(false);
    }
  }

  const handleTestConnection = async (providerKey: string) => {
    try {
      setTestingProvider(providerKey);
      setCheckResults((prev) => ({ ...prev, [providerKey]: null }));
      console.log("[v0] Testing provider connection:", providerKey);

      const result = await testProviderConnection(providerKey);
      console.log("[v0] Provider test result:", result);

      if (result.rateLimited) {
        showToast(
          `Rate limited. Please try again in ${
            result.retryAfterSeconds || 60
          } seconds.`,
          "error"
        );
        setCheckResults((prev) => ({ ...prev, [providerKey]: "failed" }));
        setTimeout(() => {
          setCheckResults((prev) => ({ ...prev, [providerKey]: null }));
        }, 3000);
        return;
      }

      if (result.available) {
        setCheckResults((prev) => ({ ...prev, [providerKey]: "success" }));
        setTimeout(() => {
          setCheckResults((prev) => ({ ...prev, [providerKey]: null }));
        }, 3000);
      } else {
        showToast(
          result.message ||
            "Provider is not available or not responding correctly",
          "error"
        );
        setCheckResults((prev) => ({ ...prev, [providerKey]: "failed" }));
        setTimeout(() => {
          setCheckResults((prev) => ({ ...prev, [providerKey]: null }));
        }, 3000);
      }
    } catch (err: any) {
      console.error("[v0] Provider test error:", err);
      showToast(err.message || "Failed to test provider connection", "error");
      setCheckResults((prev) => ({ ...prev, [providerKey]: "failed" }));
      setTimeout(() => {
        setCheckResults((prev) => ({ ...prev, [providerKey]: null }));
      }, 3000);
    } finally {
      setTestingProvider(null);
    }
  };

  const handleCredentialsUpdated = (
    providerKey: string,
    status: { credentialsConfigured: boolean }
  ) => {
    setProviders((prev) =>
      prev.map((p) =>
        p.key === providerKey
          ? { ...p, credentialsConfigured: status.credentialsConfigured }
          : p
      )
    );
  };

  const filteredAndSortedProviders = useMemo(() => {
    if (!Array.isArray(providers)) {
      console.error("[v0] Providers is not an array:", providers);
      return [];
    }

    let result = providers;

    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase();
      result = result.filter(
        (p) =>
          p.displayName.toLowerCase().includes(query) ||
          p.key.toLowerCase().includes(query) ||
          p.description?.toLowerCase().includes(query)
      );
    }

    result = [...result].sort((a, b) => {
      if (sortBy === "configured") {
        if (a.credentialsConfigured !== b.credentialsConfigured)
          return a.credentialsConfigured ? -1 : 1;
      }
      return a.displayName.localeCompare(b.displayName);
    });

    return result;
  }, [providers, searchQuery, sortBy]);

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <h1 className="text-3xl font-bold uppercase">PROVIDERS</h1>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {[...Array(6)].map((_, i) => (
            <div
              key={i}
              className="bg-card border-4 border-border brutal-shadow-sm p-6 animate-pulse"
            >
              <div className="h-6 bg-muted rounded w-3/4 mb-4"></div>
              <div className="h-4 bg-muted rounded w-full mb-2"></div>
              <div className="h-4 bg-muted rounded w-5/6"></div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <ErrorState
        title="Failed to load providers"
        message={error}
        onRetry={loadProviders}
      />
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4">
        <h1 className="text-3xl font-bold uppercase">PROVIDERS</h1>
        <p className="text-muted-foreground">
          Manage external data provider integrations for metadata lookup
        </p>

        <div className="flex flex-col sm:flex-row gap-3">
          <div className="relative flex-1">
            <Search
              className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground"
              size={20}
            />
            <Input
              placeholder="Search providers..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-10"
            />
          </div>
          <div className="flex gap-2">
            <Button
              variant={sortBy === "configured" ? "default" : "outline"}
              onClick={() => setSortBy("configured")}
              size="sm"
            >
              CONFIGURED FIRST
            </Button>
            <Button
              variant={sortBy === "name" ? "default" : "outline"}
              onClick={() => setSortBy("name")}
              size="sm"
            >
              NAME
            </Button>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {filteredAndSortedProviders.map((provider) => {
          const attribution = providerAttribution[provider.key] || [];
          const supportedIds = provider.supportedIdTypes || [];
          const needsCredentials =
            provider.credentialFields && provider.credentialFields.length > 0;
          const checkResult = checkResults[provider.key];

          return (
            <div
              key={provider.key}
              className="bg-card border-4 border-border brutal-shadow-sm flex flex-col"
            >
              {/* Header */}
              <div className="p-6 border-b-4 border-border">
                <div className="flex items-start justify-between gap-3 mb-3">
                  <h3 className="text-xl font-bold uppercase">
                    {provider.displayName}
                  </h3>
                  <Badge
                    variant={
                      provider.credentialsConfigured ? "default" : "outline"
                    }
                    className="shrink-0"
                  >
                    {provider.credentialsConfigured ? (
                      <>
                        <Check size={14} className="mr-1" /> CONFIGURED
                      </>
                    ) : (
                      <>
                        <AlertCircle size={14} className="mr-1" /> NOT
                        CONFIGURED
                      </>
                    )}
                  </Badge>
                </div>
                {provider.description && (
                  <p className="text-sm text-muted-foreground">
                    {provider.description}
                  </p>
                )}
              </div>

              {/* Body */}
              <div className="p-6 flex-1 space-y-4">
                {/* Data returned */}
                {provider.dataReturned && (
                  <div>
                    <p className="text-xs font-bold uppercase text-muted-foreground mb-2">
                      DATA PROVIDED
                    </p>
                    <p className="text-sm text-foreground">
                      {provider.dataReturned}
                    </p>
                  </div>
                )}

                {/* Highlights */}
                {provider.highlights && provider.highlights.length > 0 && (
                  <div>
                    <p className="text-xs font-bold uppercase text-muted-foreground mb-2">
                      HIGHLIGHTS
                    </p>
                    <ul className="space-y-1">
                      {provider.highlights.map((highlight, idx) => (
                        <li
                          key={idx}
                          className="text-sm text-foreground flex items-start gap-2"
                        >
                          <Info
                            size={14}
                            className="mt-0.5 shrink-0 text-primary"
                          />
                          <span>{highlight}</span>
                        </li>
                      ))}
                    </ul>
                  </div>
                )}

                {/* Supported identifiers */}
                {supportedIds.length > 0 && (
                  <div>
                    <p className="text-xs font-bold uppercase text-muted-foreground mb-2">
                      SUPPORTED IDS
                    </p>
                    <div className="flex flex-wrap gap-2">
                      {supportedIds.map((id) => (
                        <Badge key={id} variant="secondary" className="text-xs">
                          {id}
                        </Badge>
                      ))}
                    </div>
                  </div>
                )}

                {/* Links */}
                {(provider.websiteUrl || provider.apiUrl) && (
                  <div className="space-y-2">
                    {provider.websiteUrl && (
                      <a
                        href={provider.websiteUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="flex items-center gap-2 text-sm text-primary hover:underline"
                      >
                        <ExternalLink size={14} />
                        Website
                      </a>
                    )}
                    {provider.apiUrl && (
                      <a
                        href={provider.apiUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="flex items-center gap-2 text-sm text-primary hover:underline"
                      >
                        <ExternalLink size={14} />
                        API Documentation
                      </a>
                    )}
                  </div>
                )}
              </div>

              {/* Footer */}
              <div className="p-6 border-t-4 border-border flex items-center justify-between gap-3">
                {/* Attribution images */}
                <div className="flex items-center gap-2">
                  {attribution.map((attr, idx) =>
                    attr.href ? (
                      <a
                        key={idx}
                        href={attr.href}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="hover:opacity-80 transition-opacity"
                      >
                        <img
                          src={attr.src || "/placeholder.svg"}
                          alt={attr.alt}
                          className="h-10 object-contain"
                        />
                      </a>
                    ) : (
                      <img
                        key={idx}
                        src={attr.src || "/placeholder.svg"}
                        alt={attr.alt}
                        className="h-10 object-contain"
                      />
                    )
                  )}
                </div>

                {/* Action buttons */}
                <div className="flex gap-2">
                  {provider.credentialsConfigured && (
                    <Button
                      size="sm"
                      variant={
                        checkResult === "success"
                          ? "default"
                          : checkResult === "failed"
                          ? "destructive"
                          : "outline"
                      }
                      onClick={() => handleTestConnection(provider.key)}
                      disabled={testingProvider === provider.key}
                      className={
                        checkResult === "success"
                          ? "bg-green-600 hover:bg-green-700 text-white border-green-700"
                          : ""
                      }
                    >
                      {testingProvider === provider.key
                        ? "TESTING..."
                        : checkResult === "success"
                        ? "SUCCESS"
                        : checkResult === "failed"
                        ? "FAILED"
                        : "CHECK"}
                    </Button>
                  )}
                  {needsCredentials && (
                    <Button
                      size="sm"
                      onClick={() => setSelectedProvider(provider)}
                    >
                      CONFIGURE
                    </Button>
                  )}
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {filteredAndSortedProviders.length === 0 && (
        <div className="text-center py-12">
          <p className="text-muted-foreground">
            No providers found matching your search
          </p>
        </div>
      )}

      {/* Credentials modal */}
      {selectedProvider && (
        <ProviderCredentialsModal
          provider={selectedProvider}
          open={!!selectedProvider}
          onClose={() => setSelectedProvider(null)}
          onUpdated={(status) =>
            handleCredentialsUpdated(selectedProvider.key, status)
          }
        />
      )}
    </div>
  );
}
