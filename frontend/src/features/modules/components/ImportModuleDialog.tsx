"use client"

import type React from "react"

import { useCallback, useEffect, useRef, useState } from "react"
import { isApiError } from "../../../api/errors"
import { importModuleXml } from "../api/modulesApi"
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
import { Input } from "../../../../components/ui/input"
import { Label } from "../../../../components/ui/label"
import { FileUp, X } from "lucide-react"

type Props = {
  open: boolean
  onClose: () => void
  onModuleImported: () => void
}

export default function ImportModuleDialog({ open, onClose, onModuleImported }: Props) {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [fileError, setFileError] = useState<string | null>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (!open) {
      setLoading(false)
      setError(null)
      setSelectedFile(null)
      setFileError(null)
      if (fileInputRef.current) {
        fileInputRef.current.value = ""
      }
    }
  }, [open])

  const handleFileChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) {
      setSelectedFile(null)
      setFileError(null)
      return
    }

    if (!file.name.toLowerCase().endsWith(".xml")) {
      setFileError("Only .xml files are allowed")
      setSelectedFile(null)
      return
    }

    setFileError(null)
    setSelectedFile(file)
  }, [])

  const handleClearFile = useCallback(() => {
    setSelectedFile(null)
    setFileError(null)
    if (fileInputRef.current) {
      fileInputRef.current.value = ""
    }
  }, [])

  const handleSubmit = useCallback(
    async (e: React.FormEvent) => {
      e.preventDefault()
      if (!selectedFile) return

      setLoading(true)
      setError(null)
      try {
        await importModuleXml(selectedFile)
        onModuleImported()
        onClose()
      } catch (err) {
        const message = isApiError(err) ? err.message : "Failed to import module"
        setError(message)
      } finally {
        setLoading(false)
      }
    },
    [selectedFile, onClose, onModuleImported],
  )

  const disabled = loading || !selectedFile || !!fileError

  return (
    <Dialog open={open} onOpenChange={(isOpen) => !isOpen && onClose()}>
      <DialogContent className="sm:max-w-[500px] brutal-border brutal-shadow-sm bg-card">
        <form onSubmit={handleSubmit}>
          <DialogHeader className="border-b-4 border-border pb-4">
            <DialogTitle className="text-card-foreground uppercase">Import Module XML</DialogTitle>
            <DialogDescription>Upload a module XML file to import it into the system</DialogDescription>
          </DialogHeader>

          <div className="space-y-4 py-6">
            <div className="space-y-2">
              <Label htmlFor="module-file" className="uppercase">
                XML File *
              </Label>
              <div className="flex items-center gap-2">
                <Input
                  ref={fileInputRef}
                  id="module-file"
                  type="file"
                  accept=".xml"
                  onChange={handleFileChange}
                  disabled={loading}
                  className="brutal-border brutal-shadow-sm"
                />
              </div>
              {selectedFile && !fileError && (
                <div className="flex items-center gap-2 p-2 brutal-border bg-muted">
                  <FileUp className="h-4 w-4 text-muted-foreground" />
                  <span className="text-sm flex-1 truncate">{selectedFile.name}</span>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={handleClearFile}
                    disabled={loading}
                    className="h-6 w-6 p-0"
                  >
                    <X className="h-4 w-4" />
                  </Button>
                </div>
              )}
              {fileError && (
                <Alert variant="destructive" className="brutal-border">
                  <AlertDescription>{fileError}</AlertDescription>
                </Alert>
              )}
            </div>

            {error && (
              <Alert variant="destructive" className="brutal-border">
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}
          </div>

          <DialogFooter className="gap-2 border-t-4 border-border pt-4">
            <Button
              type="button"
              variant="outline"
              onClick={onClose}
              disabled={loading}
              className="brutal-border brutal-shadow-sm bg-card hover:bg-muted uppercase"
            >
              Cancel
            </Button>
            <Button
              type="submit"
              disabled={disabled}
              className="brutal-border brutal-shadow-sm bg-primary text-primary-foreground hover:bg-primary/90 uppercase"
            >
              {loading ? "Importing..." : "Import"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
