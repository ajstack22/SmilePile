//
//  SafariView.swift
//  SmilePile
//
//  SafariServices wrapper for opening web URLs safely.
//

import SwiftUI
import SafariServices

struct SafariView: UIViewControllerRepresentable {
    let url: URL

    func makeUIViewController(context: Context) -> SFSafariViewController {
        let controller = SFSafariViewController(url: url)
        controller.preferredControlTintColor = UIColor(red: 1.0, green: 0.42, blue: 0.42, alpha: 1.0)
        return controller
    }

    func updateUIViewController(_ uiViewController: SFSafariViewController, context: Context) {
        // No update needed
    }
}
