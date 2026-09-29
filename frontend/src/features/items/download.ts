/** Starts a file download from a same-origin URL without leaving the page; the server marks the response as an attachment. */
export function startDownload(url: string): void {
    const link = document.createElement("a")
    link.href = url
    link.rel = "noopener"
    document.body.appendChild(link)
    link.click()
    link.remove()
}
