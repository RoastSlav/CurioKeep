"use client"

import { useCallback, useEffect, useState } from "react"
import { isApiError } from "../../../api/errors"
import type { ScanModulesResponse } from "../api/modulesApi"
import { scanModulesFolder } from "../api/modulesApi"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "../../../../components/ui/dialog"
import { Button } from "../../../../components/ui/button"
import { Alert, AlertDescription } from "../../../../components/ui/alert"
import { Badge } from "../../../../components/ui/badge"
import { AlertTriangle, CheckCircle2, FolderSearch, XCircle } from "lucide-react"
import { Skeleton } from "../../../../components/ui/skeleton"

type Props = {
  open: boolean
  onClose: () => void
  onModulesScanned: () => void
}

export default function ScanModulesDialog({ open, onClose, onModulesScanned }: Props) {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [scanResult, setScanResult] = useState<ScanModulesResponse | null>(null)
  const [expandedFailed, setExpandedFailed] = useState(false)

  useEffect(() => {
    if (!open) {
      setLoading(false)
      setError(null)
      setScanResult(null)
      setExpandedFailed(false)
    }
  }, [open])

  useEffect(() => {
    if (open && !scanResult && !loading && !error) {
      handleScan()
    }
  }, [open])

  const handleScan = useCallback(async () => {
    setLoading(true)
    setError(null)
    setScanResult(null)
    try {
      const result = await scanModulesFolder()
      setScanResult(result)
      if (result.imported.length > 0) {
        onModulesScanned()
      }
    } catch (err) {
      const message = isApiError(err) ? err.message : "Failed to scan modules folder"
      setError(message)
    } finally {
      setLoading(false)
    }
  }, [onModulesScanned])

  const handleClose = useCallback(() => {
    if (scanResult && scanResult.imported.length > 0) {
      onModulesScanned()
    }
    onClose()
  }, [scanResult, onClose, onModulesScanned])

  return (
    <Dialog open={open} onOpenChange={(isOpen) => !isOpen && handleClose()}>
      <DialogContent className="sm:max-w-2xl brutal-border brutal-shadow-sm bg-card">
        <DialogHeader className="border-b-4 border-border pb-4">
          <DialogTitle className="flex items-center gap-2 text-card-foreground uppercase">
            <FolderSearch className="h-5 w-5" />
            Scan Modules Folder
          </DialogTitle>
          <DialogDescription>Scan the modules import folder for new module XML files</DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-4">
          {loading && (
            <div className="space-y-3">
              <Skeleton className="h-12 w-full" />
              <Skeleton className="h-12 w-full" />
              <Skeleton className="h-12 w-full" />
            </div>
          )}

          {error && (
            <Alert variant="destructive" className="brutal-border">
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}

          {scanResult && (
            <div className="space-y-4">
              <div className="grid gap-4 sm:grid-cols-3">
                <div className="brutal-border brutal-shadow-sm bg-green-50 dark:bg-green-950/30 p-4">
                  <div className="flex items-center gap-2 mb-2">
                    <CheckCircle2 className="h-5 w-5 text-green-600 dark:text-green-400" />
                    <span className="font-bold uppercase text-sm">Imported</span>
                  </div>
                  <div className="text-2xl font-bold text-green-600 dark:text-green-400">
                    {scanResult.imported.length}
                  </div>
                </div>

                <div className="brutal-border brutal-shadow-sm bg-blue-50 dark:bg-blue-950/30 p-4">
                  <div className="flex items-center gap-2 mb-2">
                    <AlertTriangle className="h-5 w-5 text-blue-600 dark:text-blue-400" />
                    <span className="font-bold uppercase text-sm">Skipped</span>
                  </div>
                  <div className="text-2xl font-bold text-blue-600 dark:text-blue-400">{scanResult.skipped.length}</div>
                </div>

                <div className="brutal-border brutal-shadow-sm bg-red-50 dark:bg-red-950/30 p-4">
                  <div className="flex items-center gap-2 mb-2">
                    <XCircle className="h-5 w-5 text-red-600 dark:text-red-400" />
                    <span className="font-bold uppercase text-sm">Failed</span>
                  </div>
                  <div className="text-2xl font-bold text-red-600 dark:text-red-400">{scanResult.failed.length}</div>
                </div>
              </div>

              {scanResult.imported.length > 0 && (
                <div className="brutal-border brutal-shadow-sm bg-muted p-4">
                  <h3 className="font-bold mb-2 uppercase">Imported Modules</h3>
                  <div className="space-y-2">
                    {scanResult.imported.map((module) => (
                      <div key={module.id} className="flex items-center justify-between gap-2 text-sm">
                        <span className="font-medium truncate">{module.name}</span>
                        <Badge variant="outline" className="brutal-border uppercase shrink-0">
                          {module.version}
                        </Badge>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {scanResult.skipped.length > 0 && (
                <div className="brutal-border brutal-shadow-sm bg-muted p-4">
                  <h3 className="font-bold mb-2 uppercase">Skipped Files</h3>
                  <div className="flex flex-wrap gap-2">
                    {scanResult.skipped.map((file, idx) => (
                      <Badge key={idx} variant="outline" className="brutal-border text-xs">
                        {file}
                      </Badge>
                    ))}
                  </div>
                </div>
              )}

              {scanResult.failed.length > 0 && (
                <div className="brutal-border brutal-shadow-sm bg-muted p-4">
                  <button
                    type="button"
                    onClick={() => setExpandedFailed(!expandedFailed)}
                    className="font-bold mb-2 uppercase flex items-center gap-2 hover:text-primary transition-colors"
                  >
                    <span>Failed Imports ({scanResult.failed.length})</span>
                    <span className="text-xs">{expandedFailed ? "▼" : "▶"}</span>
                  </button>
                  {expandedFailed && (
                    <div className="space-y-2 mt-2">
                      {scanResult.failed.map((failure, idx) => (
                        <div key={idx} className="brutal-border bg-card p-3 text-sm">
                          <div className="font-medium text-destructive">{failure.source}</div>
                          <div className="text-muted-foreground text-xs mt-1">{failure.reason}</div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}
            </div>
          )}
        </div>

        <DialogFooter className="gap-2 border-t-4 border-border pt-4">
          {scanResult ? (
            <Button
              onClick={handleClose}
              className="brutal-border brutal-shadow-sm bg-primary text-primary-foreground hover:bg-primary/90 uppercase"
            >
              Close
            </Button>
          ) : (
            <>
              <Button
                variant="outline"
                onClick={onClose}
                disabled={loading}
                className="brutal-border brutal-shadow-sm bg-card hover:bg-muted uppercase"
              >
                Cancel
              </Button>
              <Button
                onClick={handleScan}
                disabled={loading}
                className="brutal-border brutal-shadow-sm bg-primary text-primary-foreground hover:bg-primary/90 uppercase"
              >
                {loading ? "Scanning..." : "Retry Scan"}
              </Button>
            </>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
