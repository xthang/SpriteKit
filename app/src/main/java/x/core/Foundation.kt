package x.core

typealias TimeInterval = Double

enum class NSLineBreakMode {
	byWordWrapping, // Wrap at word boundaries, default
	byCharWrapping, // Wrap at character boundaries
	byClipping, // Simply clip
	byTruncatingHead, // Truncate at head of line: "...wxyz"
	byTruncatingTail, // Truncate at tail of line: "abcd..."
	byTruncatingMiddle // Truncate middle of line:  "ab...yz"
}